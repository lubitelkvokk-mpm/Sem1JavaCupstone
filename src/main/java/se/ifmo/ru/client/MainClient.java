package se.ifmo.ru.client;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.AuthResponseDto;
import se.ifmo.ru.common.dto.DeleteRequestDto;
import se.ifmo.ru.common.dto.DeleteResponseDto;
import se.ifmo.ru.common.dto.ErrorDto;
import se.ifmo.ru.common.dto.LinkInfoDto;
import se.ifmo.ru.common.dto.ListResponseDto;
import se.ifmo.ru.common.dto.NotificationDto;
import se.ifmo.ru.common.dto.NotificationsResponseDto;
import se.ifmo.ru.common.dto.RedirectRequestDto;
import se.ifmo.ru.common.dto.RedirectResponseDto;
import se.ifmo.ru.common.dto.ShortenRequestDto;
import se.ifmo.ru.common.dto.ShortenResponseDto;
import se.ifmo.ru.common.dto.UpdateRequestDto;
import se.ifmo.ru.common.dto.UpdateResponseDto;
import se.ifmo.ru.infra.AppConfig;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;

/**
 * Консольный клиент сервиса сокращения ссылок.
 */
public class MainClient {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private final AppConfig config;
    private final Scanner scanner;
    private ClientSession session;
    private UUID user;

    public static void main(String[] args) throws IOException {
        new MainClient(AppConfig.load(), new Scanner(System.in, StandardCharsets.UTF_8)).run();
    }

    MainClient(AppConfig config, Scanner scanner) {
        this.config = config;
        this.scanner = scanner;
    }

    void run() throws IOException {
        try {
            session = new ClientSession(config.host(), config.port());
            user = authenticate();
            System.out.println("Ваш идентификатор (UUID): " + user);
            printHelp();
            loop();
        } catch (IOException e) {
            System.err.println("Не удалось подключиться к серверу "
                    + config.host() + ":" + config.port() + ": " + e.getMessage());
            System.err.println("Запустите сервер: mvn compile exec:java -Dexec.mainClass=se.ifmo.ru.server.MainServer");
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (IOException ignored) {
                    // уже закрыто
                }
            }
        }
    }

    private UUID authenticate() throws IOException {
        UUID stored = UserUuidStore.load();
        Message response = session.send(new Message(UUID.randomUUID(), stored,
                MessageType.AUTH_REQ, null));
        if (response.type() == MessageType.ERROR) {
            throw new IOException(errorText(response));
        }
        UUID uuid = MessageManager.payload(response, AuthResponseDto.class).userUuid();
        UserUuidStore.save(uuid);
        return uuid;
    }

    private void loop() throws IOException {
        while (true) {
            System.out.print("\n> ");
            String line = scanner.nextLine();
            if (line == null || line.isBlank()) {
                continue;
            }
            String[] parts = line.trim().split("\\s+", 2);
            String cmd = parts[0].toLowerCase(Locale.ROOT);
            String arg = parts.length > 1 ? parts[1].trim() : "";
            try {
                switch (cmd) {
                    case "0", "exit", "quit" -> {
                        System.out.println("Пока!");
                        return;
                    }
                    case "1", "shorten" -> doShorten(arg);
                    case "2", "open" -> doOpen(arg);
                    case "3", "list" -> doList();
                    case "4", "update" -> doUpdate(arg);
                    case "5", "delete" -> doDelete(arg);
                    case "6", "notifications" -> doNotifications();
                    case "h", "help" -> printHelp();
                    default -> System.out.println(
                            "Неизвестная команда: " + cmd + ". Введите 'help' для списка команд.");
                }
            } catch (IOException e) {
                System.err.println("Ошибка соединения: " + e.getMessage());
                return;
            }
        }
    }

    private void doShorten(String arg) throws IOException {
        String url = arg;
        Integer maxVisits = null;
        if (url.isEmpty()) {
            System.out.print("Введите URL для сокращения: ");
            url = scanner.nextLine().trim();
        } else {
            String[] parts = arg.split("\\s+", 2);
            url = parts[0];
            if (parts.length > 1 && !parts[1].isBlank()) {
                maxVisits = parseVisits(parts[1]);
                if (maxVisits == null) {
                    return;
                }
            }
        }
        if (url.isEmpty()) {
            System.out.println("URL не может быть пустым");
            return;
        }
        Message response = session.send(new Message(UUID.randomUUID(), user,
                MessageType.SHORTEN_REQ,
                MessageManager.payload(new ShortenRequestDto(url, maxVisits))));
        if (response.type() == MessageType.ERROR) {
            System.out.println("Ошибка: " + errorText(response));
            return;
        }
        ShortenResponseDto dto = MessageManager.payload(response, ShortenResponseDto.class);
        System.out.println("Готово! Короткая ссылка: " + dto.shortUrl());
        System.out.println("  исходный URL:   " + dto.originalUrl());
        System.out.println("  лимит переходов: " + dto.maxVisits());
        System.out.println("  истечёт:        " + TIME_FORMAT.format(dto.expiresAt()));
        System.out.println("  открыть:        open " + dto.shortCode());
    }

    private void doOpen(String arg) throws IOException {
        String code = extractCode(arg);
        if (code == null) {
            return;
        }
        Message response = session.send(new Message(UUID.randomUUID(), user,
                MessageType.REDIRECT_REQ,
                MessageManager.payload(new RedirectRequestDto(code))));
        if (response.type() == MessageType.ERROR) {
            System.out.println("Ошибка: " + errorText(response));
            return;
        }
        RedirectResponseDto dto = MessageManager.payload(response, RedirectResponseDto.class);
        switch (dto.status()) {
            case OK -> {
                System.out.println("Переход учтён, осталось переходов: " + dto.remainingVisits());
                System.out.println("Открываю: " + dto.originalUrl());
                openBrowser(dto.originalUrl());
            }
            case LIMIT_EXCEEDED -> System.out.println(
                    "УВЕДОМЛЕНИЕ: лимит переходов по ссылке исчерпан, ссылка заблокирована.");
            case EXPIRED -> System.out.println(
                    "УВЕДОМЛЕНИЕ: срок жизни ссылки истёк, ссылка удалена.");
            case NOT_FOUND -> System.out.println("Ссылка с кодом " + code + " не найдена.");
        }
    }

    private void doList() throws IOException {
        Message response = session.send(new Message(UUID.randomUUID(), user,
                MessageType.LIST_REQ, null));
        if (response.type() == MessageType.ERROR) {
            System.out.println("Ошибка: " + errorText(response));
            return;
        }
        ListResponseDto dto = MessageManager.payload(response, ListResponseDto.class);
        if (dto.links().isEmpty()) {
            System.out.println("У вас пока нет ссылок. Создайте: shorten <url> [лимит]");
            return;
        }
        System.out.printf("%-10s %-9s %-15s %s%n", "КОД", "ПЕРЕХОДЫ", "СТАТУС", "ССЫЛКА");
        for (LinkInfoDto link : dto.links()) {
            System.out.printf("%-10s %-9s %-15s %s%n",
                    link.shortCode(), link.visits() + "/" + link.maxVisits(),
                    link.status(), link.url());
        }
    }

    private void doUpdate(String arg) throws IOException {
        String[] parts = arg.split("\\s+", 2);
        String code = parts.length > 0 ? parts[0] : "";
        if (code.isEmpty()) {
            System.out.print("Введите код ссылки: ");
            code = scanner.nextLine().trim();
        }
        if (code.isEmpty()) {
            return;
        }
        Integer maxVisits = null;
        if (parts.length > 1 && !parts[1].isBlank()) {
            maxVisits = parseVisits(parts[1]);
            if (maxVisits == null) {
                return;
            }
        } else {
            System.out.print("Новый лимит переходов: ");
            maxVisits = parseVisits(scanner.nextLine().trim());
            if (maxVisits == null) {
                return;
            }
        }
        Message response = session.send(new Message(UUID.randomUUID(), user,
                MessageType.UPDATE_REQ,
                MessageManager.payload(new UpdateRequestDto(code, maxVisits))));
        if (response.type() == MessageType.ERROR) {
            System.out.println("Ошибка: " + errorText(response));
            return;
        }
        UpdateResponseDto dto = MessageManager.payload(response, UpdateResponseDto.class);
        if (dto.error() != null) {
            System.out.println("Ошибка: " + dto.error());
            return;
        }
        System.out.println("Лимит обновлён: " + dto.link().shortUrl()
                + ", переходов: " + dto.link().visits() + "/" + dto.link().maxVisits());
    }

    private void doDelete(String arg) throws IOException {
        String code = arg;
        if (code.isEmpty()) {
            System.out.print("Введите код ссылки для удаления: ");
            code = scanner.nextLine().trim();
        }
        if (code.isEmpty()) {
            return;
        }
        Message response = session.send(new Message(UUID.randomUUID(), user,
                MessageType.DELETE_REQ,
                MessageManager.payload(new DeleteRequestDto(code))));
        if (response.type() == MessageType.ERROR) {
            System.out.println("Ошибка: " + errorText(response));
            return;
        }
        DeleteResponseDto dto = MessageManager.payload(response, DeleteResponseDto.class);
        if (dto.deleted()) {
            System.out.println("Ссылка " + dto.code() + " удалена.");
        } else {
            System.out.println("Ошибка: " + dto.error());
        }
    }

    private void doNotifications() throws IOException {
        Message response = session.send(new Message(UUID.randomUUID(), user,
                MessageType.NOTIFICATIONS_REQ, null));
        if (response.type() == MessageType.ERROR) {
            System.out.println("Ошибка: " + errorText(response));
            return;
        }
        NotificationsResponseDto dto = MessageManager.payload(response, NotificationsResponseDto.class);
        if (dto.notifications().isEmpty()) {
            System.out.println("Нет новых уведомлений.");
            return;
        }
        System.out.println("Новые уведомления:");
        for (NotificationDto n : dto.notifications()) {
            System.out.println("  [" + TIME_FORMAT.format(n.createdAt()) + "] " + n.text());
        }
    }

    private Integer parseVisits(String value) {
        try {
            int v = Integer.parseInt(value);
            if (v < 1) {
                System.out.println("Лимит переходов должен быть не меньше 1");
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            System.out.println("Лимит переходов должен быть целым числом, получено: " + value);
            return null;
        }
    }

    /**
     * Принимает код или полную короткую ссылку (clck.ru/XXXXXX) и возвращает код.
     */
    private String extractCode(String arg) {
        if (arg == null || arg.isBlank()) {
            System.out.print("Введите код или короткую ссылку: ");
            arg = scanner.nextLine().trim();
        }
        if (arg.isEmpty()) {
            return null;
        }
        int slash = arg.lastIndexOf('/');
        return slash >= 0 ? arg.substring(slash + 1) : arg;
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.out.println("Открытие браузера недоступно в этой среде. URL: " + url);
            }
        } catch (Exception e) {
            System.out.println("Не удалось открыть браузер: " + e.getMessage() + ". URL: " + url);
        }
    }

    private String errorText(Message response) {
        return MessageManager.payload(response, ErrorDto.class).message();
    }

    private void printHelp() {
        System.out.println("""
                Команды:
                  shorten <url> [лимит]   — сократить ссылку (лимит опционален)
                  open <код>              — перейти по короткой ссылке (откроется браузер)
                  list                    — показать мои ссылки
                  update <код> [лимит]    — изменить лимит переходов
                  delete <код>            — удалить ссылку
                  notifications           — показать новые уведомления
                  help                    — эта справка
                  exit                    — выйти

                Примеры:
                  shorten https://www.baeldung.com/java-9-http-client 5
                  open 3DZHeG
                  update 3DZHeG 10
                """);
    }
}
