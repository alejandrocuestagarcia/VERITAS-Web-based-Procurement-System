package com.veritas.backend.notification.dto;

import lombok.Data;

@Data
public class NotificationDto {
    private Long id;
    private String message;
    private boolean isRead;
}
