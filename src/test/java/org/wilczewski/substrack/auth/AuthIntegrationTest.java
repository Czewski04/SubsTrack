package org.wilczewski.substrack.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.wilczewski.substrack.AbstractIntegrationTest;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Test
    void shouldRegisterUserAndReturnToken() throws Exception {
        TestUser user = uniqueUser();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken", not(blankOrNullString())));
    }

    @Test
    void shouldRejectDuplicateRegistration() throws Exception {
        TestUser user = uniqueUser();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(user)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(user)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username or email already exists"));
    }

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        TestUser user = uniqueUser();
        register(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.email(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken", not(blankOrNullString())));
    }

    @Test
    void shouldRejectLoginWithBadPassword() throws Exception {
        TestUser user = uniqueUser();
        register(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.email(), "WrongPassword123!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    private void register(TestUser user) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(user)))
                .andExpect(status().isOk());
    }

    private TestUser uniqueUser() {
        String suffix = UUID.randomUUID().toString();
        return new TestUser("user-" + suffix, "user-" + suffix + "@example.com");
    }

    private String registerBody(TestUser user) {
        return """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(user.username(), user.email(), PASSWORD, PASSWORD);
    }

    private String loginBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private record TestUser(String username, String email) {
    }
}
