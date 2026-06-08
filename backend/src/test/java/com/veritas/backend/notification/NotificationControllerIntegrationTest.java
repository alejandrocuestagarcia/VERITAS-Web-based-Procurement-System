package com.veritas.backend.notification;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.notification.entity.Notification;
import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.notification.repository.NotificationRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class NotificationControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        testUser = userRepository.findByEmail("notif-test@veritas.com").orElseGet(() -> {
            User u = User.builder()
                    .name("Notification Test User")
                    .email("notif-test@veritas.com")
                    .passwordHash(encoder.encode("password"))
                    .role(UserRole.REQUESTER)
                    .isActive(true)
                    .build();
            return userRepository.save(u);
        });

        otherUser = userRepository.findByEmail("notif-other@veritas.com").orElseGet(() -> {
            User u = User.builder()
                    .name("Other Test User")
                    .email("notif-other@veritas.com")
                    .passwordHash(encoder.encode("password"))
                    .role(UserRole.REQUESTER)
                    .isActive(true)
                    .build();
            return userRepository.save(u);
        });
    }

    @Test
    void getAllNotifications_ReturnsPagedNotifications() throws Exception {
        createNotification(testUser, NotificationType.SUBMITTED, "Test notification 1");
        createNotification(testUser, NotificationType.APPROVED, "Test notification 2");

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].type").exists())
                .andExpect(jsonPath("$.content[0].message").exists());
    }

    @Test
    void getUnreadCount_ReturnsCorrectCount() throws Exception {
        createNotification(testUser, NotificationType.SUBMITTED, "Unread 1");
        createNotification(testUser, NotificationType.APPROVED, "Unread 2");

        Notification readNotif = createNotification(testUser, NotificationType.FINISHED, "Read");
        readNotif.setIsRead(true);
        notificationRepository.save(readNotif);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void markAsRead_OwnNotification_ReturnsNoContent() throws Exception {
        Notification notif = createNotification(testUser, NotificationType.SUBMITTED, "Mark me as read");

        mockMvc.perform(patch("/api/v1/notifications/" + notif.getNotificationId() + "/read")
                        .with(user(testUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void markAsRead_OtherUsersNotification_ReturnsForbidden() throws Exception {
        Notification notif = createNotification(otherUser, NotificationType.SUBMITTED, "Not yours");

        mockMvc.perform(patch("/api/v1/notifications/" + notif.getNotificationId() + "/read")
                        .with(user(testUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void markAllAsRead_ReturnsNoContent() throws Exception {
        createNotification(testUser, NotificationType.SUBMITTED, "Unread 1");
        createNotification(testUser, NotificationType.APPROVED, "Unread 2");

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .with(user(testUser)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void deleteNotification_OwnNotification_ReturnsNoContent() throws Exception {
        Notification notif = createNotification(testUser, NotificationType.SUBMITTED, "Delete me");

        mockMvc.perform(delete("/api/v1/notifications/" + notif.getNotificationId())
                        .with(user(testUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNotification_OtherUsersNotification_ReturnsForbidden() throws Exception {
        Notification notif = createNotification(otherUser, NotificationType.SUBMITTED, "Not yours");

        mockMvc.perform(delete("/api/v1/notifications/" + notif.getNotificationId())
                        .with(user(testUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllNotifications_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllNotifications_AsFinanceOfficer_ReturnsOk() throws Exception {
        User financeOfficer = userRepository.findByEmail("finance-test@veritas.com").orElseGet(() -> {
            User u = User.builder()
                    .name("Finance Officer Test User")
                    .email("finance-test@veritas.com")
                    .passwordHash(encoder.encode("password"))
                    .role(UserRole.FINANCE_OFFICER)
                    .isActive(true)
                    .build();
            return userRepository.save(u);
        });

        createNotification(financeOfficer, NotificationType.ASSIGNED, "Awaiting payment");

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user(financeOfficer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getEmailPreference_ReturnsTrueByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/email-preference")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void updateEmailPreference_TogglesValueAndUpdatesDatabase() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/email-preference")
                        .param("enabled", "false")
                        .with(user(testUser)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/email-preference")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    private Notification createNotification(User user, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .request(null)
                .build();
        return notificationRepository.save(notification);
    }
}
