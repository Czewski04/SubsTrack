package org.wilczewski.substrack.user.api.dto.response;

import java.util.UUID;

public record UserCredentialsResponse(
        UUID id,
        String email,
        String passwordHash
) {
}
