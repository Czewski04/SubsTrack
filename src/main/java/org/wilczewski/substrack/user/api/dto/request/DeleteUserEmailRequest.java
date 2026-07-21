package org.wilczewski.substrack.user.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeleteUserEmailRequest(
        @NotNull(message = "Email ID cannot be null")
        UUID emailId
) {}
