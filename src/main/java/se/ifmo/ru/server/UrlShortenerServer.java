package se.ifmo.ru.server;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.ErrorDto;
import se.ifmo.ru.core.NotificationService;
import se.ifmo.ru.core.ShortCodeGenerator;
import se.ifmo.ru.core.ShortLink;
import se.ifmo.ru.core.UrlShortenerService;
import se.ifmo.ru.infra.AppConfig;
import se.ifmo.ru.infra.InMemoryLinkRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * TCP-сервис сокращения ссылок на Java NIO.
 *
 * <p>Один поток обслуживает всех клиентов: неблокирующий
 * ServerSocketChannel зарегистрирован в Selector,
 * новые подключения принимаются событием OP_ACCEPT, данные клиентов
 * читаются/пишутся асинхронно (OP_READ/OP_WRITE). Чтение и запись
 * могут быть частичными, поэтому для каждого соединения ведётся
 * буфер накопления входящих данных и буфер исходящих.</p>
 */
public class UrlShortenerServer implements AutoCloseable {

    private final AppConfig config;
    private final UrlShortenerService service;
    private final CommandDispatcher dispatcher;
    private final ServerSocketChannel serverChannel;
    private final Selector selector;
    private volatile boolean running;

    /**
     * Состояние одного клиентского соединения.
     */
    private static final class ClientConnection {
        final SocketChannel channel;
        final StringBuilder readBuffer = new StringBuilder();
        ByteBuffer writeBuffer = ByteBuffer.allocate(8192);

        ClientConnection(SocketChannel channel) {
            this.channel = channel;
        }
    }

    public UrlShortenerServer(AppConfig config) throws IOException {
        this.config = config;
        this.service = new UrlShortenerService(
                new InMemoryLinkRepository(),
                new NotificationService(),
                new ShortCodeGenerator(config.codeLength()),
                config);
        this.dispatcher = new CommandDispatcher(service, config);
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverChannel.bind(new InetSocketAddress(config.host(), config.port()));
        this.selector = Selector.open();
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public UrlShortenerService service() {
        return service;
    }

    public int actualPort() {
        try {
            return ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось определить порт сервера", e);
        }
    }

    public void start() {
        running = true;
        startPurgeThread();
        System.out.println("Сервер запущен на " + config.host() + ":" + actualPort());
        System.out.println("TTL ссылки: " + config.ttlSeconds() + " с, "
                + "лимит переходов по умолчанию: " + config.defaultMaxVisits()
                + ", автоудаление каждые " + Math.max(1, config.purgeIntervalSeconds()) + " с");
    }

    /**
     * Основной NIO-цикл событий: accept → read → dispatch → write.
     */
    public void eventLoop() {
        while (running) {
            try {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove(); // обязательно удаляем из selected-набора
                    if (!key.isValid()) {
                        continue;
                    }
                    try {
                        if (key.isAcceptable()) {
                            accept(key);
                        } else if (key.isReadable()) {
                            read(key);
                        } else if (key.isWritable()) {
                            write(key);
                        }
                    } catch (IOException e) {
                        closeClient(key);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Ошибка селектора: " + e.getMessage());
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
        if (client == null) {
            return;
        }
        client.configureBlocking(false);
        ClientConnection conn = new ClientConnection(client);
        client.register(selector, SelectionKey.OP_READ, conn);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientConnection conn = (ClientConnection) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int n;
        while ((n = channel.read(buffer)) > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            buffer.clear();
            conn.readBuffer.append(new String(bytes, StandardCharsets.UTF_8));
        }
        if (n == -1) {
            // клиент закрыл соединение
            closeClient(key);
            return;
        }
        processLines(conn, key);
    }

    /**
     * Разбирает накопленные данные по строкам: одна строка — одно
     * JSON-сообщение протокола.
     */
    private void processLines(ClientConnection conn, SelectionKey key) {
        String data = conn.readBuffer.toString();
        int start = 0;
        int idx;
        while ((idx = data.indexOf('\n', start)) >= 0) {
            String line = data.substring(start, idx).strip();
            start = idx + 1;
            if (!line.isEmpty()) {
                appendWrite(conn, key, toResponse(line));
            }
        }
        conn.readBuffer.replace(0, conn.readBuffer.length(), data.substring(start));
    }

    private String toResponse(String line) {
        try {
            Message request = MessageManager.fromJson(line);
            return MessageManager.toJson(dispatcher.dispatch(request));
        } catch (IllegalArgumentException e) {
            ErrorDto error = new ErrorDto("BAD_REQUEST", "Некорректный JSON сообщения");
            return MessageManager.toJson(new Message(UUID.randomUUID(), null,
                    MessageType.ERROR, MessageManager.payload(error)));
        }
    }

    /**
     * Добавляет ответ в исходящий буфер соединения и включает OP_WRITE.
     */
    private void appendWrite(ClientConnection conn, SelectionKey key, String line) {
        byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = conn.writeBuffer;
        if (buf.remaining() < bytes.length) {
            // не хватает места: расширяем буфер, сохранив ещё не отправленные данные
            int pending = buf.position();
            ByteBuffer expanded = ByteBuffer.allocate(
                    Math.max(buf.capacity() * 2, pending + bytes.length));
            expanded.put(buf.array(), 0, pending);
            conn.writeBuffer = expanded;
        }
        conn.writeBuffer.put(bytes);
        key.interestOps(key.isReadable()
                ? SelectionKey.OP_READ | SelectionKey.OP_WRITE
                : SelectionKey.OP_WRITE);
    }

    /**
     * Отправляет накопленные данные. В неблокирующем режиме write()
     * может отправить только часть — остаток дошлём по следующему OP_WRITE.
     */
    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientConnection conn = (ClientConnection) key.attachment();
        ByteBuffer buf = conn.writeBuffer;
        buf.flip();
        int n = channel.write(buf);
        if (n < 0) {
            // канал закрыт — сбрасываем соединение
            closeClient(key);
            return;
        }
        buf.compact();
        if (buf.position() == 0) {
            // всё отправлено — снова ждём только данные клиента
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void closeClient(SelectionKey key) {
        try {
            key.channel().close();
        } catch (IOException ignored) {
            // уже закрыт
        }
        key.cancel();
    }

    private void startPurgeThread() {
        Thread purge = new Thread(this::purgeLoop, "purge");
        purge.setDaemon(true);
        purge.start();
    }

    private void purgeLoop() {
        long intervalMillis = Math.max(1, config.purgeIntervalSeconds()) * 1000;
        while (running) {
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            purgeSafely();
        }
    }

    private void purgeSafely() {
        try {
            List<ShortLink> removed = service.purgeExpired();
            if (!removed.isEmpty()) {
                System.out.println("[PURGE] Удалено протухших ссылок: " + removed.size());
            }
        } catch (Exception e) {
            System.err.println("[PURGE] Ошибка автоудаления: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            selector.close();
        } catch (IOException ignored) {
            // уже закрыт
        }
        try {
            serverChannel.close();
        } catch (IOException ignored) {
            // уже закрыт
        }
    }
}
