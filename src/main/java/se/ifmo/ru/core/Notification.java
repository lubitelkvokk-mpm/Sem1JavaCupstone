package se.ifmo.ru.core;

import java.time.Instant;
import java.util.UUID;

/**
 * Уведомление владельца ссылки (лимит исчерпан, срок жизни истёк).
 */
public class Notification {

    private final UUID id;
    private final UUID user;
    private final String text;
    private final Instant createdAt;
    private volatile boolean read;

    public Notification(UUID user, String text, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.text = text;
        this.createdAt = createdAt;
        this.read = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUser() {
        return user;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void markAsRead() {
        this.read = true;
    }
}
