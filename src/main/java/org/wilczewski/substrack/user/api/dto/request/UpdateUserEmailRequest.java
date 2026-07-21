package org.wilczewski.substrack.user.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateUserEmailRequest(
        @NotNull(message = "Email ID cannot be null")
        UUID emailId,
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        String newEmail
) {}
