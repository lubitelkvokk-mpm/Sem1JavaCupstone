package se.ifmo.ru.common.dto;

/**
 * @param link  обновлённая ссылка (null, если операция не выполнена)
 * @param error текст ошибки (null, если операция успешна)
 */
public record UpdateResponseDto(LinkInfoDto link, String error) {
}
