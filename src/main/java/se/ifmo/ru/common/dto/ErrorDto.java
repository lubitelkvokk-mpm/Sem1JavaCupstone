package se.ifmo.ru.common.dto;

/**
 * @param code    машинный код ошибки
 * @param message человекочитаемое описание
 */
public record ErrorDto(String code, String message) {
}
