package org.wilczewski.substrack.user.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
        String username
) {}
