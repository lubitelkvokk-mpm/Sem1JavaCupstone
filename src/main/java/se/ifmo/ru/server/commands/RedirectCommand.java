package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.RedirectRequestDto;
import se.ifmo.ru.common.dto.RedirectResponseDto;
import se.ifmo.ru.core.ResolveResult;
import se.ifmo.ru.server.ServerContext;

/**
 * Переход по короткой ссылке (проверка лимита и срока жизни, учёт перехода).
 */
public class RedirectCommand implements Command {

    @Override
    public MessageType type() {
        return MessageType.REDIRECT_REQ;
    }

    @Override
    public Message handle(Message request, ServerContext ctx) {
        RedirectRequestDto req = MessageManager.payload(request, RedirectRequestDto.class);
        ResolveResult result = ctx.service().resolve(req.code());
        RedirectResponseDto dto = new RedirectResponseDto(
                result.status(), result.originalUrl(), result.remainingVisits());
        return new Message(request.id(), request.userUuid(), MessageType.REDIRECT_RESP,
                MessageManager.payload(dto));
    }
}
