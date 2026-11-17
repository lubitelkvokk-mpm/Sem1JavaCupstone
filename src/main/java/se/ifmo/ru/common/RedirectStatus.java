package se.ifmo.ru.common;

/**
 * Результат попытки перехода по короткой ссылке.
 */
public enum RedirectStatus {
    OK,
    LIMIT_EXCEEDED,
    EXPIRED,
    NOT_FOUND
}
