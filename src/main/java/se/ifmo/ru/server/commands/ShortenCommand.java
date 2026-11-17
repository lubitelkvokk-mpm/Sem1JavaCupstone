package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.ShortenRequestDto;
import se.ifmo.ru.common.dto.ShortenResponseDto;
import se.ifmo.ru.core.ShortLink;
import se.ifmo.ru.server.ServerContext;

/**
 * Создание короткой ссылки.
 */
public class ShortenCommand implements Command {

    @Override
    public MessageType type() {
        return MessageType.SHORTEN_REQ;
    }

    @Override
    public Message handle(Message request, ServerContext ctx) {
        ShortenRequestDto req = MessageManager.payload(request, ShortenRequestDto.class);
        ShortLink link = ctx.service().createLink(request.userUuid(), req.url(), req.maxVisits());
        ShortenResponseDto dto = new ShortenResponseDto(
                request.userUuid(),
                link.getCode(),
                ctx.service().shortUrl(link),
                link.getUrl(),
                link.getMaxVisits(),
                link.getCreatedAt(),
                link.getExpiresAt());
        return new Message(request.id(), request.userUuid(), MessageType.SHORTEN_RESP,
                MessageManager.payload(dto));
    }
}
