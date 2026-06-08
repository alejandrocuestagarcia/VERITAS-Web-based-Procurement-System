package com.veritas.backend.notification;

import com.veritas.backend.notification.dto.NotificationDto;
import com.veritas.backend.notification.entity.Notification;
import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.notification.mapper.NotificationMapper;
import com.veritas.backend.notification.repository.NotificationRepository;
import com.veritas.backend.notification.service.impl.NotificationServiceImpl;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.veritas.backend.notification.service.NotificationEmailService;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationEmailService notificationEmailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Request testRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@veritas.com")
                .build();

        testRequest = new Request();
        testRequest.setRequestID(10L);
        testRequest.setRequestKey("PROJ-1");
        testRequest.setRequestName("Test Request");
    }

    @Test
    void createNotification_ValidInput_SavesNotification() {
        notificationService.createNotification(testUser, testRequest, NotificationType.SUBMITTED, "Test message");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getMessage()).isEqualTo("Test message");
        assertThat(saved.getType()).isEqualTo(NotificationType.SUBMITTED);
        assertThat(saved.getRequest()).isEqualTo(testRequest);
        assertThat(saved.getIsRead()).isFalse();

        verify(notificationEmailService).sendNotificationEmail(testUser, testRequest, NotificationType.SUBMITTED, "Test message");
    }

    @Test
    void createNotification_NullRequest_SavesWithNullRequestFields() {
        notificationService.createNotification(testUser, null, NotificationType.ASSIGNED, "Generic message");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRequest()).isNull();

        verify(notificationEmailService).sendNotificationEmail(testUser, null, NotificationType.ASSIGNED, "Generic message");
    }

    @Test
    void getNotificationsForUser_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification = Notification.builder()
                .notificationId(1L)
                .user(testUser)
                .message("Test")
                .type(NotificationType.APPROVED)
                .createdAt(LocalDateTime.now())
                .build();
        Page<Notification> page = new PageImpl<>(List.of(notification));

        NotificationDto dto = NotificationDto.builder()
                .id(1L)
                .message("Test")
                .type("APPROVED")
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);
        when(notificationMapper.toDto(notification)).thenReturn(dto);

        Page<NotificationDto> result = notificationService.getNotificationsForUser(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo("APPROVED");
    }

    @Test
    void getUnreadCount_ReturnsCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void markAsRead_OwnNotification_MarksAsRead() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .user(testUser)
                .isRead(false)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 1L);

        assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_OtherUsersNotification_ThrowsAccessDenied() {
        User otherUser = User.builder().id(2L).build();
        Notification notification = Notification.builder()
                .notificationId(1L)
                .user(otherUser)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void markAsRead_NotificationNotFound_ThrowsEntityNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void markAllAsRead_CallsRepository() {
        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllAsReadByUserId(1L);
    }

    @Test
    void deleteNotification_OwnNotification_Deletes() {
        Notification notification = Notification.builder()
                .notificationId(1L)
                .user(testUser)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(1L, 1L);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_OtherUsersNotification_ThrowsAccessDenied() {
        User otherUser = User.builder().id(2L).build();
        Notification notification = Notification.builder()
                .notificationId(1L)
                .user(otherUser)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.deleteNotification(1L, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
