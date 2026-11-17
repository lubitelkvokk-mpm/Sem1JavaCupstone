package se.ifmo.ru.common.dto;

/**
 * @param code    код удалённой ссылки
 * @param deleted true, если ссылка удалена
 * @param error   текст ошибки (null, если операция успешна)
 */
public record DeleteResponseDto(String code, boolean deleted, String error) {
}
