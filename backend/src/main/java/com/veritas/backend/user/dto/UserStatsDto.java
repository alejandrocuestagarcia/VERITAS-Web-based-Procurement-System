package com.veritas.backend.user.dto;

public record UserStatsDto(long total, long inactive, int activeSessions) {
}
