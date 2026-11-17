package se.ifmo.ru.core;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Короткая ссылка: код, владелец, лимит переходов, время жизни.
 */
public class ShortLink {

    private final String code;
    private final UUID owner;
    private final String url;
    private final int maxVisits;
    private final AtomicInteger visitCount;
    private final Instant createdAt;
    private final Instant expiresAt;

    public ShortLink(String code, UUID owner, String url, int maxVisits, Instant createdAt, Instant expiresAt) {
        this(code, owner, url, maxVisits, 0, createdAt, expiresAt);
    }

    public ShortLink(String code, UUID owner, String url, int maxVisits, int visitCount,
                     Instant createdAt, Instant expiresAt) {
        this.code = code;
        this.owner = owner;
        this.url = url;
        this.maxVisits = maxVisits;
        this.visitCount = new AtomicInteger(visitCount);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getCode() {
        return code;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getUrl() {
        return url;
    }

    public int getMaxVisits() {
        return maxVisits;
    }

    public int getVisitCount() {
        return visitCount.get();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isLimitReached() {
        return visitCount.get() >= maxVisits;
    }

    /**
     * Попытка учесть один переход. Атомарно: учитывает переход только
     * если лимит ещё не исчерпан.
     *
     * @return true, если переход учтён
     */
    public boolean tryIncrementVisit() {
        while (true) {
            int current = visitCount.get();
            if (current >= maxVisits) {
                return false;
            }
            if (visitCount.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }
}
