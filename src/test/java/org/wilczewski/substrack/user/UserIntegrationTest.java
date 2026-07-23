package org.wilczewski.substrack.user;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.wilczewski.substrack.AbstractIntegrationTest;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Test
    void shouldReturnAuthenticatedUser() throws Exception {
        TestUser user = uniqueUser();
        String token = registerAndGetToken(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.username()))
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.additionalEmails[0].email").value(user.email()))
                .andExpect(jsonPath("$.additionalEmails[0].id").isNotEmpty());
    }

    @Test
    void shouldUpdateProfileAndPassword() throws Exception {
        TestUser user = uniqueUser();
        String token = registerAndGetToken(user);
        String newUsername = "updated-" + UUID.randomUUID();
        String newPassword = "NewPassword123!";

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s"}
                                """.formatted(newUsername)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "%s",
                                  "newPassword": "%s",
                                  "confirmNewPassword": "%s"
                                }
                                """.formatted(PASSWORD, newPassword, newPassword)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(newUsername));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.email(), PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.email(), newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").isNotEmpty());
    }

    @Test
    void shouldAddUpdateAndDeleteAdditionalEmail() throws Exception {
        TestUser user = uniqueUser();
        String token = registerAndGetToken(user);
        String additionalEmail = uniqueEmail("additional");
        String updatedEmail = uniqueEmail("updated");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/me/emails")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}
                                """.formatted(additionalEmail)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/v1/users/me/emails/")))
                .andReturn();

        String location = createResult.getResponse().getHeader(HttpHeaders.LOCATION);
        UUID emailId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalEmails[*].email", hasItem(additionalEmail)));

        mockMvc.perform(put("/api/v1/users/me/emails")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailId": "%s",
                                  "newEmail": "%s"
                                }
                                """.formatted(emailId, updatedEmail)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalEmails[*].email", hasItem(updatedEmail)))
                .andExpect(jsonPath("$.additionalEmails[*].email", not(hasItem(additionalEmail))));

        mockMvc.perform(delete("/api/v1/users/me/emails")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailId": "%s"}
                                """.formatted(emailId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalEmails[*].email", not(hasItem(updatedEmail))));
    }

    @Test
    void shouldRejectDeletingPrimaryEmail() throws Exception {
        TestUser user = uniqueUser();
        String token = registerAndGetToken(user);

        MvcResult meResult = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String primaryEmailId = JsonPath.read(
                meResult.getResponse().getContentAsString(),
                "$.additionalEmails[0].id"
        );

        mockMvc.perform(delete("/api/v1/users/me/emails")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailId": "%s"}
                                """.formatted(primaryEmailId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot delete primary email"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.additionalEmails[*].email", hasItem(user.email())));
    }

    private String registerAndGetToken(TestUser user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s",
                                  "confirmPassword": "%s"
                                }
                                """.formatted(user.username(), user.email(), PASSWORD, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.jwtToken");
    }

    private String loginBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private TestUser uniqueUser() {
        String suffix = UUID.randomUUID().toString();
        return new TestUser("user-" + suffix, "user-" + suffix + "@example.com");
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private record TestUser(String username, String email) {
    }
}
