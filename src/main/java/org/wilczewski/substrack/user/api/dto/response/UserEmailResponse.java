package org.wilczewski.substrack.user.api.dto.response;

import java.util.UUID;

public record UserEmailResponse(
        UUID id,
        String email
) {}
