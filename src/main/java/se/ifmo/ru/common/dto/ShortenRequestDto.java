package se.ifmo.ru.common.dto;

/**
 * @param url       исходный длинный URL
 * @param maxVisits лимит переходов (null — использовать значение по умолчанию)
 */
public record ShortenRequestDto(String url, Integer maxVisits) {
}
