package se.ifmo.ru.common.dto;

import java.time.Instant;

public record NotificationDto(String text, Instant createdAt) {
}
