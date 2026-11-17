package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.LinkStatus;
import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.LinkInfoDto;
import se.ifmo.ru.common.dto.ListResponseDto;
import se.ifmo.ru.core.ShortLink;
import se.ifmo.ru.server.ServerContext;

import java.time.Instant;
import java.util.List;

/**
 * Список ссылок текущего пользователя.
 */
public class ListCommand implements Command {

    @Override
    public MessageType type() {
        return MessageType.LIST_REQ;
    }

    @Override
    public Message handle(Message request, ServerContext ctx) {
        Instant now = Instant.now();
        List<ShortLink> links = ctx.service().listLinks(request.userUuid());
        List<LinkInfoDto> dtos = links.stream()
                .map(link -> toInfo(ctx, link, now))
                .toList();
        return new Message(request.id(), request.userUuid(), MessageType.LIST_RESP,
                MessageManager.payload(new ListResponseDto(dtos)));
    }

    private LinkInfoDto toInfo(ServerContext ctx, ShortLink link, Instant now) {
        LinkStatus status;
        if (link.isExpired(now)) {
            status = LinkStatus.EXPIRED;
        } else if (link.isLimitReached()) {
            status = LinkStatus.LIMIT_EXCEEDED;
        } else {
            status = LinkStatus.ACTIVE;
        }
        return new LinkInfoDto(
                link.getCode(),
                ctx.service().shortUrl(link),
                link.getUrl(),
                link.getMaxVisits(),
                link.getVisitCount(),
                status,
                link.getCreatedAt(),
                link.getExpiresAt());
    }
}
