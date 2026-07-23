package org.wilczewski.substrack.notification.internal;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.MvcResult;
import org.wilczewski.substrack.AbstractIntegrationTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationSendIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationSentRepository notificationSentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM notification_durations");
        jdbcTemplate.update("DELETE FROM notification_sent");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM subscriptions");
        jdbcTemplate.update("DELETE FROM user_emails");
        jdbcTemplate.update("DELETE FROM users");
        clearInvocations(mailSender);
    }

    @Test
    void shouldSendDueReminderFromDatabaseAndRemainIdempotent() throws Exception {
        TestUser user = registerUniqueUser();
        LocalDate billingDate = LocalDate.now().plusDays(1);
        UUID subscriptionId = createSubscriptionDueTomorrow(user, billingDate.atTime(12, 0));
        createNotificationDueToday(user.token(), subscriptionId);
        clearInvocations(mailSender);

        notificationService.sendNotifications();

        assertEquals(1, notificationSentRepository.count());
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertNotNull(message.getTo());
        assertEquals(user.email(), message.getTo()[0]);
        assertEquals("Due subscription - Payment reminder", message.getSubject());
        assertNotNull(message.getText());
        assertTrue(message.getText().contains("Due subscription"));
        assertTrue(message.getText().contains(billingDate.toString()));

        notificationService.sendNotifications();

        assertEquals(1, notificationSentRepository.count());
        verify(mailSender, times(1)).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    private UUID createSubscriptionDueTomorrow(TestUser user, LocalDateTime startDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Due subscription",
                                  "emailId": "%s",
                                  "price": 15.00,
                                  "currency": "USD",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "period": 1,
                                  "periodType": "MONTH",
                                  "isActive": true,
                                  "includeTrail": false,
                                  "trailLength": 0
                                }
                                """.formatted(user.emailId(), startDate, startDate.plusYears(1))))
                .andExpect(status().isCreated())
                .andReturn();
        return idFromLocation(result);
    }

    private void createNotificationDueToday(String token, UUID subscriptionId) throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/{subscriptionId}/notifications", subscriptionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isActive": true,
                                  "durations": ["P1D"]
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private TestUser registerUniqueUser() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = "send-" + suffix + "@example.com";
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "send-%s",
                                  "email": "%s",
                                  "password": "%s",
                                  "confirmPassword": "%s"
                                }
                                """.formatted(suffix, email, PASSWORD, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(registration.getResponse().getContentAsString(), "$.jwtToken");

        MvcResult profile = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String emailId = JsonPath.read(profile.getResponse().getContentAsString(), "$.additionalEmails[0].id");
        return new TestUser(token, email, UUID.fromString(emailId));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private UUID idFromLocation(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private record TestUser(String token, String email, UUID emailId) {
    }
}
