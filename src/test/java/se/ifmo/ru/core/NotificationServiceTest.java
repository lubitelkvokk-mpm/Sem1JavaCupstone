package se.ifmo.ru.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private final NotificationService service = new NotificationService();
    private final UUID user = UUID.randomUUID();

    @Test
    void notifyAddsNotification() {
        service.notify(user, "тест");
        List<Notification> unread = service.markAsRead(user);
        assertEquals(1, unread.size());
        assertEquals("тест", unread.get(0).getText());
        assertEquals(user, unread.get(0).getUser());
    }

    @Test
    void markAsReadReturnsUnreadAndMarksThem() {
        service.notify(user, "первое");
        service.notify(user, "второе");
        List<Notification> first = service.markAsRead(user);
        assertEquals(2, first.size());
        assertTrue(service.markAsRead(user).isEmpty());
    }

    @Test
    void markAsReadForUnknownUserReturnsEmpty() {
        assertTrue(service.markAsRead(UUID.randomUUID()).isEmpty());
    }

    @Test
    void notificationsAreIsolatedPerUser() {
        UUID other = UUID.randomUUID();
        service.notify(user, "для первого");
        service.notify(other, "для второго");
        assertEquals(1, service.markAsRead(user).size());
        assertEquals(1, service.markAsRead(other).size());
    }
}
