package org.wilczewski.substrack.auth.api.dto.command;

public record RegisterCommand(
        String username,
        String email,
        String password,
        String confirmPassword
) {}
