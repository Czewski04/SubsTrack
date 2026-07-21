package org.wilczewski.substrack.user.api.dto.command;

import java.util.UUID;

public record UpdateUserEmailCommand(
        UUID userId,
        UUID emailId,
        String newEmail
) {}
