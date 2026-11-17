package se.ifmo.ru.common.dto;

import se.ifmo.ru.common.LinkStatus;

import java.time.Instant;

public record LinkInfoDto(
        String shortCode,
        String shortUrl,
        String url,
        int maxVisits,
        int visits,
        LinkStatus status,
        Instant createdAt,
        Instant expiresAt) {
}
