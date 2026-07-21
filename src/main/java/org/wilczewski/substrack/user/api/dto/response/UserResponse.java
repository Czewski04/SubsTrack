package org.wilczewski.substrack.user.api.dto.response;

import java.util.List;

public record UserResponse(
        String username,
        String email,
        List<UserEmailResponse> additionalEmails
) {}
