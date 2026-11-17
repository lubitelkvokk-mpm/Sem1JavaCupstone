package se.ifmo.ru.common;

import java.util.UUID;

/**
 * Базовое сообщение протокола.
 *
 * @param id       уникальный идентификатор запроса (эхо в ответе)
 * @param userUuid идентификатор пользователя (для AUTH_REQ может быть null)
 * @param type     тип сообщения
 * @param payload  JSON-строка с параметрами команды (может быть null)
 */
public record Message(UUID id, UUID userUuid, MessageType type, String payload) {
}
