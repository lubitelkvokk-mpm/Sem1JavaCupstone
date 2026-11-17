package se.ifmo.ru.server;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.ErrorDto;
import se.ifmo.ru.core.InvalidUrlException;
import se.ifmo.ru.core.UrlShortenerService;
import se.ifmo.ru.infra.AppConfig;
import se.ifmo.ru.server.commands.AuthorizeCommand;
import se.ifmo.ru.server.commands.Command;
import se.ifmo.ru.server.commands.DeleteCommand;
import se.ifmo.ru.server.commands.ListCommand;
import se.ifmo.ru.server.commands.NotificationsCommand;
import se.ifmo.ru.server.commands.RedirectCommand;
import se.ifmo.ru.server.commands.ShortenCommand;
import se.ifmo.ru.server.commands.UpdateCommand;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Маршрутизация запросов на команды и единая обработка ошибок.
 */
public class CommandDispatcher {

    private final ServerContext ctx;
    private final Map<MessageType, Command> commands;

    public CommandDispatcher(UrlShortenerService service, AppConfig config) {
        this.ctx = new ServerContext(service, config);
        this.commands = List.of(
                        new AuthorizeCommand(),
                        new ShortenCommand(),
                        new RedirectCommand(),
                        new ListCommand(),
                        new UpdateCommand(),
                        new DeleteCommand(),
                        new NotificationsCommand())
                .stream()
                .collect(Collectors.toMap(Command::type, Function.identity()));
    }

    public Message dispatch(Message request) {
        Command command = commands.get(request.type());
        if (command == null) {
            return error(request, "UNKNOWN_TYPE", "Неизвестный тип сообщения: " + request.type());
        }
        try {
            return command.handle(request, ctx);
        } catch (InvalidUrlException e) {
            return error(request, "INVALID_URL", e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(request, "BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка обработки " + request.type() + ": " + e);
            return error(request, "INTERNAL_ERROR", "Внутренняя ошибка сервера");
        }
    }

    private Message error(Message request, String code, String message) {
        ErrorDto dto = new ErrorDto(code, message);
        return new Message(request.id(), request.userUuid(), MessageType.ERROR,
                MessageManager.payload(dto));
    }
}
