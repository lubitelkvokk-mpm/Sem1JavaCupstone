package se.ifmo.ru.client;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Сессия клиента на Java NIO: неблокирующий {@link SocketChannel}
 * и {@link Selector}.
 *
 * <p>Каждый запрос выполняется на отдельном соединении:
 * OP_CONNECT → finishConnect() → OP_WRITE (отправка запроса порциями)
 * → OP_READ (накопление ответа в ByteBuffer) → разбор JSON-строки.
 * Как и в неблокирующем режиме, read/write могут быть частичными,
 * поэтому данные обрабатываются циклами до готовности.</p>
 */
public class ClientSession implements AutoCloseable {

    private static final long TIMEOUT_MILLIS = 10_000;

    private final InetSocketAddress address;

    public ClientSession(String host, int port) {
        this.address = new InetSocketAddress(host, port);
    }

    /**
     * Отправляет запрос и дожидается одного ответа (неблокирующий I/O).
     */
    public Message send(Message message) throws IOException {
        ByteBuffer writeBuf = ByteBuffer.wrap(
                (MessageManager.toJson(message) + "\n").getBytes(StandardCharsets.UTF_8));

        try (Selector selector = Selector.open();
             SocketChannel channel = SocketChannel.open()) {

            channel.configureBlocking(false);
            channel.connect(address);
            channel.register(selector, SelectionKey.OP_CONNECT);

            StringBuilder response = new StringBuilder();
            ByteBuffer readBuf = ByteBuffer.allocate(8192);
            long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;

            while (System.currentTimeMillis() < deadline) {
                if (selector.select(1000) == 0) {
                    continue; // таймаут select — ещё не готово, пробуем снова
                }
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove(); // обязательно удаляем из selected-набора
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isConnectable()) {
                        if (channel.finishConnect()) {
                            key.interestOps(SelectionKey.OP_WRITE);
                        } else {
                            throw new IOException("Не удалось подключиться к серверу");
                        }
                    }
                    if (key.isWritable()) {
                        // write() в неблокирующем режиме может отправить не всё
                        while (writeBuf.hasRemaining()) {
                            int n = channel.write(writeBuf);
                            if (n < 0) {
                                throw new IOException("Ошибка записи: соединение закрыто");
                            }
                            if (n == 0) {
                                break; // дождёмся следующего OP_WRITE
                            }
                        }
                        if (!writeBuf.hasRemaining()) {
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                    if (key.isReadable()) {
                        int n;
                        while ((n = channel.read(readBuf)) > 0) {
                            readBuf.flip();
                            byte[] bytes = new byte[readBuf.remaining()];
                            readBuf.get(bytes);
                            readBuf.clear();
                            response.append(new String(bytes, StandardCharsets.UTF_8));
                            int nl = response.indexOf("\n");
                            if (nl >= 0) {
                                // получили полную строку-ответ
                                return MessageManager.fromJson(
                                        response.substring(0, nl).strip());
                            }
                        }
                        if (n == -1) {
                            // сервер закрыл соединение
                            String data = response.toString().strip();
                            if (data.isEmpty()) {
                                throw new IOException("Сервер закрыл соединение без ответа");
                            }
                            return MessageManager.fromJson(data);
                        }
                    }
                }
            }
            throw new IOException("Таймаут ожидания ответа сервера (" + TIMEOUT_MILLIS + " мс)");
        }
    }

    @Override
    public void close() throws IOException {
        // каждое соединение закрывается после запроса, закрывать нечего
    }
}
