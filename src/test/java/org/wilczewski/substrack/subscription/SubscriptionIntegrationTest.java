package org.wilczewski.substrack.subscription;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.wilczewski.substrack.AbstractIntegrationTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionIntegrationTest extends AbstractIntegrationTest {

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
    void shouldCreateReadListUpdateAndDeleteSubscription() throws Exception {
        TestUser user = registerUniqueUser();
        LocalDateTime startDate = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime endDate = startDate.plusMonths(6);

        UUID subscriptionId = createSubscription(
                user, "Streaming", new BigDecimal("19.99"), startDate, endDate);

        mockMvc.perform(get("/api/v1/subscriptions/{id}", subscriptionId)
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.name").value("Streaming"))
                .andExpect(jsonPath("$.emailId").value(user.emailId().toString()))
                .andExpect(jsonPath("$.price").value(19.99))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.period").value(1))
                .andExpect(jsonPath("$.periodType").value("MONTH"))
                .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(get("/api/v1/subscriptions")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(subscriptionId.toString()));

        mockMvc.perform(put("/api/v1/subscriptions/{id}", subscriptionId)
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionBody(
                                "Updated streaming", user.emailId(), new BigDecimal("24.50"),
                                startDate.plusDays(1), endDate.plusMonths(1))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/subscriptions/{id}", subscriptionId)
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated streaming"))
                .andExpect(jsonPath("$.price").value(24.5));

        mockMvc.perform(delete("/api/v1/subscriptions/{id}", subscriptionId)
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/subscriptions/{id}", subscriptionId)
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectSubscriptionWhoseStartDateIsAfterEndDate() throws Exception {
        TestUser user = registerUniqueUser();
        LocalDateTime startDate = LocalDateTime.now().plusDays(10).withNano(0);

        mockMvc.perform(post("/api/v1/subscriptions")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionBody(
                                "Invalid dates", user.emailId(), BigDecimal.TEN,
                                startDate, startDate.minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start date cannot be after end date"));
    }

    @Test
    void shouldForbidAccessToAnotherUsersSubscription() throws Exception {
        TestUser owner = registerUniqueUser();
        TestUser anotherUser = registerUniqueUser();
        LocalDateTime startDate = LocalDateTime.now().plusDays(2).withNano(0);
        UUID subscriptionId = createSubscription(
                owner, "Owner subscription", BigDecimal.TEN, startDate, startDate.plusMonths(1));

        mockMvc.perform(get("/api/v1/subscriptions/{id}", subscriptionId)
                        .header("Authorization", bearer(anotherUser.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Subscription does not belong to user"));

        mockMvc.perform(delete("/api/v1/subscriptions/{id}", subscriptionId)
                        .header("Authorization", bearer(anotherUser.token())))
                .andExpect(status().isForbidden());
    }

    private UUID createSubscription(
            TestUser user,
            String name,
            BigDecimal price,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionBody(name, user.emailId(), price, startDate, endDate)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/subscriptions/")))
                .andReturn();

        return idFromLocation(result);
    }

    private TestUser registerUniqueUser() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String username = "user-" + suffix;
        String email = "user-" + suffix + "@example.com";
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, email)))
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

    private String subscriptionBody(
            String name,
            UUID emailId,
            BigDecimal price,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return """
                {
                  "name": "%s",
                  "emailId": "%s",
                  "price": %s,
                  "currency": "USD",
                  "startDate": "%s",
                  "endDate": "%s",
                  "period": 1,
                  "periodType": "MONTH",
                  "isActive": true,
                  "includeTrail": false,
                  "trailLength": 0
                }
                """.formatted(name, emailId, price, startDate, endDate);
    }

    private String registerBody(String username, String email) {
        return """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(username, email, PASSWORD, PASSWORD);
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
