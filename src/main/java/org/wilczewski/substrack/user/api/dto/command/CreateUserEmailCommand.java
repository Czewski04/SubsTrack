package org.wilczewski.substrack.user.api.dto.command;

import java.util.UUID;

public record CreateUserEmailCommand(
        UUID userId,
        String email
) {}
