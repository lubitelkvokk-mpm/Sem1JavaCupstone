package se.ifmo.ru.common.dto;

/**
 * @param code      код короткой ссылки
 * @param maxVisits новое значение лимита переходов
 */
public record UpdateRequestDto(String code, Integer maxVisits) {
}
