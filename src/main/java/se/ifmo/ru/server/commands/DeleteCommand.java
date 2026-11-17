package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.DeleteRequestDto;
import se.ifmo.ru.common.dto.DeleteResponseDto;
import se.ifmo.ru.server.ServerContext;

/**
 * Удаление ссылки (только владельцем).
 */
public class DeleteCommand implements Command {

    @Override
    public MessageType type() {
        return MessageType.DELETE_REQ;
    }

    @Override
    public Message handle(Message request, ServerContext ctx) {
        DeleteRequestDto req = MessageManager.payload(request, DeleteRequestDto.class);
        boolean deleted = ctx.service().deleteLink(request.userUuid(), req.code());
        DeleteResponseDto dto = new DeleteResponseDto(req.code(), deleted,
                deleted ? null : "Ссылка не найдена или принадлежит другому пользователю");
        return new Message(request.id(), request.userUuid(), MessageType.DELETE_RESP,
                MessageManager.payload(dto));
    }
}
