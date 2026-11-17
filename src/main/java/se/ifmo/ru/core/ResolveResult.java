package se.ifmo.ru.core;

import se.ifmo.ru.common.RedirectStatus;

/**
 * Результат попытки перехода по короткой ссылке.
 */
public record ResolveResult(RedirectStatus status, String originalUrl, int remainingVisits) {

    public static ResolveResult ok(String originalUrl, int remainingVisits) {
        return new ResolveResult(RedirectStatus.OK, originalUrl, remainingVisits);
    }

    public static ResolveResult limitExceeded() {
        return new ResolveResult(RedirectStatus.LIMIT_EXCEEDED, null, 0);
    }

    public static ResolveResult expired() {
        return new ResolveResult(RedirectStatus.EXPIRED, null, 0);
    }

    public static ResolveResult notFound() {
        return new ResolveResult(RedirectStatus.NOT_FOUND, null, 0);
    }
}
