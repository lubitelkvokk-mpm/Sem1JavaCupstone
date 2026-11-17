package se.ifmo.ru.common.dto;

import java.time.Instant;
import java.util.UUID;

public record ShortenResponseDto(
        UUID userUuid,
        String shortCode,
        String shortUrl,
        String originalUrl,
        int maxVisits,
        Instant createdAt,
        Instant expiresAt) {
}
