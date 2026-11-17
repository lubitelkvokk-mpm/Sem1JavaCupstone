package se.ifmo.ru.common.dto;

import se.ifmo.ru.common.RedirectStatus;

public record RedirectResponseDto(RedirectStatus status, String originalUrl, int remainingVisits) {
}
