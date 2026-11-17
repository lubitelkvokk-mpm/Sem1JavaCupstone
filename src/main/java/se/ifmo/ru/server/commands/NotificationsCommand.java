package se.ifmo.ru.server.commands;

import se.ifmo.ru.common.Message;
import se.ifmo.ru.common.MessageManager;
import se.ifmo.ru.common.MessageType;
import se.ifmo.ru.common.dto.NotificationDto;
import se.ifmo.ru.common.dto.NotificationsResponseDto;
import se.ifmo.ru.core.Notification;
import se.ifmo.ru.server.ServerContext;

import java.util.List;

/**
 * Непрочитанные уведомления пользователя (лимит исчерпан, TTL истёк).
 */
public class NotificationsCommand implements Command {

    @Override
    public MessageType type() {
        return MessageType.NOTIFICATIONS_REQ;
    }

    @Override
    public Message handle(Message request, ServerContext ctx) {
        List<Notification> unread =
                ctx.service().notifications().markAsRead(request.userUuid());
        List<NotificationDto> dtos = unread.stream()
                .map(n -> new NotificationDto(n.getText(), n.getCreatedAt()))
                .toList();
        return new Message(request.id(), request.userUuid(), MessageType.NOTIFICATIONS_RESP,
                MessageManager.payload(new NotificationsResponseDto(dtos)));
    }
}
