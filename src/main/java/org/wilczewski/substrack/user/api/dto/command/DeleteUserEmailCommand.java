package org.wilczewski.substrack.user.api.dto.command;

import java.util.UUID;

public record DeleteUserEmailCommand(
        UUID userId,
        UUID emailId
) {
}
