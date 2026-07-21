package org.wilczewski.substrack.user.api.dto.command;

public record CreateUserCommand(
        String username,
        String email,
        String passwordHash
) {}
