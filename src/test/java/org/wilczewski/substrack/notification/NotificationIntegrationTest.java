package org.wilczewski.substrack.notification;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.wilczewski.substrack.AbstractIntegrationTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

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
    }

    @Test
    void shouldCreateReadUpdateAndDeleteNotification() throws Exception {
        TestUser user = registerUniqueUser();
        UUID subscriptionId = createSubscription(user, "Notification CRUD");
        UUID notificationId = createNotification(user.token(), subscriptionId, true, "P1D", "P3D");

        mockMvc.perform(get(notificationUrl(subscriptionId))
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.durations", containsInAnyOrder("PT24H", "PT72H")));

        mockMvc.perform(put(notificationUrl(subscriptionId))
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateNotificationBody(notificationId, false, "P2D")))
                .andExpect(status().isOk());

        mockMvc.perform(get(notificationUrl(subscriptionId))
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.durations[0]").value("PT48H"));

        mockMvc.perform(delete(notificationUrl(subscriptionId))
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": "%s"}
                                """.formatted(notificationId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(notificationUrl(subscriptionId))
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectSecondNotificationForSameSubscription() throws Exception {
        TestUser user = registerUniqueUser();
        UUID subscriptionId = createSubscription(user, "Conflict");
        createNotification(user.token(), subscriptionId, true, "P1D");

        mockMvc.perform(post(notificationUrl(subscriptionId))
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createNotificationBody(true, "P2D")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Notification for this subscription already exists"));
    }

    @Test
    void shouldEnforceSubscriptionAndNotificationOwnership() throws Exception {
        TestUser owner = registerUniqueUser();
        TestUser anotherUser = registerUniqueUser();
        UUID ownersSubscription = createSubscription(owner, "Owned");
        UUID ownersNotification = createNotification(owner.token(), ownersSubscription, true, "P1D");
        UUID otherOwnedSubscription = createSubscription(owner, "Another owned subscription");

        mockMvc.perform(get(notificationUrl(ownersSubscription))
                        .header("Authorization", bearer(anotherUser.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Subscription does not belong to user"));

        mockMvc.perform(put(notificationUrl(otherOwnedSubscription))
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateNotificationBody(ownersNotification, true, "P2D")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Notification does not belong to subscription"));
    }

    private UUID createNotification(
            String token,
            UUID subscriptionId,
            boolean active,
            String... durations
    ) throws Exception {
        MvcResult result = mockMvc.perform(post(notificationUrl(subscriptionId))
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createNotificationBody(active, durations)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith(notificationUrl(subscriptionId) + "/")))
                .andReturn();
        return idFromLocation(result);
    }

    private UUID createSubscription(TestUser user, String name) throws Exception {
        LocalDateTime startDate = LocalDateTime.now().plusDays(2).withNano(0);
        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "emailId": "%s",
                                  "price": 12.99,
                                  "currency": "USD",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "period": 1,
                                  "periodType": "MONTH",
                                  "isActive": true,
                                  "includeTrail": false,
                                  "trailLength": 0
                                }
                                """.formatted(name, user.emailId(), startDate, startDate.plusYears(1))))
                .andExpect(status().isCreated())
                .andReturn();
        return idFromLocation(result);
    }

    private TestUser registerUniqueUser() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = "user-" + suffix + "@example.com";
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user-%s",
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
        return new TestUser(token, UUID.fromString(emailId));
    }

    private String createNotificationBody(boolean active, String... durations) {
        return """
                {
                  "isActive": %s,
                  "durations": [%s]
                }
                """.formatted(active, quoted(durations));
    }

    private String updateNotificationBody(UUID id, boolean active, String... durations) {
        return """
                {
                  "id": "%s",
                  "isActive": %s,
                  "durations": [%s]
                }
                """.formatted(id, active, quoted(durations));
    }

    private String quoted(String[] values) {
        return java.util.Arrays.stream(values)
                .map(value -> "\"" + value + "\"")
                .collect(java.util.stream.Collectors.joining(","));
    }

    private String notificationUrl(UUID subscriptionId) {
        return "/api/v1/subscriptions/" + subscriptionId + "/notifications";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private UUID idFromLocation(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private record TestUser(String token, UUID emailId) {
    }
}
