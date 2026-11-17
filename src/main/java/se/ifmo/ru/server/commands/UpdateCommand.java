package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.LinkStatus;
import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.LinkInfoDto;
import se.ifmo.ru.common.dto.UpdateRequestDto;
import se.ifmo.ru.common.dto.UpdateResponseDto;
import se.ifmo.ru.core.ShortLink;
import se.ifmo.ru.server.ServerContext;

import java.time.Instant;
import java.util.Optional;

/**
 * Изменение лимита переходов ссылки (только владельцем).
 */
public class UpdateCommand implements Command {

    @Override
    public MessageType type() {
        return MessageType.UPDATE_REQ;
    }

    @Override
    public Message handle(Message request, ServerContext ctx) {
        UpdateRequestDto req = MessageManager.payload(request, UpdateRequestDto.class);
        Optional<ShortLink> updated =
                ctx.service().updateLink(request.userUuid(), req.code(), req.maxVisits());
        if (updated.isEmpty()) {
            UpdateResponseDto dto = new UpdateResponseDto(null,
                    "Ссылка не найдена или принадлежит другому пользователю");
            return new Message(request.id(), request.userUuid(), MessageType.UPDATE_RESP,
                    MessageManager.payload(dto));
        }
        ShortLink link = updated.get();
        Instant now = Instant.now();
        LinkStatus status = link.isExpired(now) ? LinkStatus.EXPIRED
                : link.isLimitReached() ? LinkStatus.LIMIT_EXCEEDED : LinkStatus.ACTIVE;
        LinkInfoDto info = new LinkInfoDto(
                link.getCode(),
                ctx.service().shortUrl(link),
                link.getUrl(),
                link.getMaxVisits(),
                link.getVisitCount(),
                status,
                link.getCreatedAt(),
                link.getExpiresAt());
        return new Message(request.id(), request.userUuid(), MessageType.UPDATE_RESP,
                MessageManager.payload(new UpdateResponseDto(info, null)));
    }
}
