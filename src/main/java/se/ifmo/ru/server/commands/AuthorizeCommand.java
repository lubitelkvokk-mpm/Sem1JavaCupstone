package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.AuthResponseDto;
import se.ifmo.ru.server.ServerContext;

import java.util.UUID;

/**
 * Выдаёт (или подтверждает) UUID пользователя.
 */
public class AuthorizeCommand implements Command {

    @Override
    public MessageType type() {
        return MessageType.AUTH_REQ;
    }

    @Override
    public Message handle(Message request, ServerContext ctx) {
        UUID user = ctx.service().authenticate(request.userUuid());
        Message response = new Message(request.id(), user, MessageType.AUTH_RESP,
                MessageManager.payload(new AuthResponseDto(user)));
        return response;
    }
}
