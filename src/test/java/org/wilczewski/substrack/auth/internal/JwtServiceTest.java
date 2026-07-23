package org.wilczewski.substrack.auth.internal;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET =
            "unit-test-secret-key-with-at-least-32-bytes";

    @Test
    void generatedTokenContainsUserIdAndIsValid() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId);

        assertEquals(userId, jwtService.extractUserId(token));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService jwtService = new JwtService(SECRET, -1_000);
        String token = jwtService.generateToken(UUID.randomUUID());

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void malformedTokenIsInvalid() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        assertFalse(jwtService.isTokenValid("not-a-jwt"));
    }
}
