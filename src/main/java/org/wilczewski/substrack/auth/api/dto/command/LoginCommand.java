package org.wilczewski.substrack.auth.api.dto.command;

public record LoginCommand(
        String email,
        String password
) {
}
