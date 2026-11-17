package se.ifmo.ru.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Хранилище уведомлений пользователей. Уведомления также дублируются
 * в лог сервера, чтобы события не терялись.
 */
public class NotificationService {

    private final Map<UUID, List<Notification>> byUser = new ConcurrentHashMap<>();

    public void notify(UUID user, String text) {
        byUser.computeIfAbsent(user, k -> new CopyOnWriteArrayList<>())
                .add(new Notification(user, text, Instant.now()));
        System.out.println("[УВЕДОМЛЕНИЕ] пользователь " + user + ": " + text);
    }

    /**
     * Возвращает непрочитанные уведомления пользователя и помечает их прочитанными.
     */
    public List<Notification> markAsRead(UUID user) {
        List<Notification> all = byUser.getOrDefault(user, List.of());
        List<Notification> unread = new ArrayList<>();
        for (Notification n : all) {
            if (!n.isRead()) {
                unread.add(n);
            }
        }
        unread.forEach(Notification::markAsRead);
        return unread;
    }
}
