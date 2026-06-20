package com.veritas.backend.notification.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.veritas.backend.notification.dto.NotificationDto;
import com.veritas.backend.notification.entity.Notification;
import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.requisition.entity.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

class NotificationMapperUnitTest {

    private NotificationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(NotificationMapper.class);
    }

    @Test
    void toDto_NullNotification_ReturnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDto_NullRequest_MapsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = Notification.builder()
            .notificationId(1L)
            .message("Testing message")
            .isRead(true)
            .createdAt(now)
            .type(NotificationType.APPROVED)
            .request(null)
            .build();

        NotificationDto dto = mapper.toDto(notification);

        assertAll(
            () -> assertEquals(1L, dto.getId()),
            () -> assertEquals("Testing message", dto.getMessage()),
            () -> assertTrue(dto.isRead()),
            () -> assertEquals(now, dto.getCreatedAt()),
            () -> assertEquals("APPROVED", dto.getType()),
            () -> assertNull(dto.getRequestId()),
            () -> assertNull(dto.getRequestKey()),
            () -> assertNull(dto.getRequestName())
        );
    }

    @Test
    void toDto_WithRequest_MapsCorrectly() {
        Request request = new Request();
        request.setRequestID(42L);
        request.setRequestKey("REQ-42");
        request.setRequestName("Requisition for laptops");

        LocalDateTime now = LocalDateTime.now();
        Notification notification = Notification.builder()
            .notificationId(2L)
            .message("New request submitted")
            .isRead(false)
            .createdAt(now)
            .type(NotificationType.SUBMITTED)
            .request(request)
            .build();

        NotificationDto dto = mapper.toDto(notification);

        assertAll(
            () -> assertEquals(2L, dto.getId()),
            () -> assertEquals("New request submitted", dto.getMessage()),
            () -> assertFalse(dto.isRead()),
            () -> assertEquals(now, dto.getCreatedAt()),
            () -> assertEquals("SUBMITTED", dto.getType()),
            () -> assertEquals(42L, dto.getRequestId()),
            () -> assertEquals("REQ-42", dto.getRequestKey()),
            () -> assertEquals("Requisition for laptops", dto.getRequestName())
        );
    }
}
