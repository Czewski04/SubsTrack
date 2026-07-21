package org.wilczewski.substrack.user.api.dto.command;

import java.util.UUID;

public record UpdateUserPasswordCommand(
        UUID userId,
        String password,
        String newPassword,
        String confirmNewPassword
) {}
