package org.wilczewski.substrack.user.api;

import org.wilczewski.substrack.user.api.dto.command.CreateUserCommand;
import org.wilczewski.substrack.user.api.dto.response.UserCredentialsResponse;

import java.util.UUID;

public interface UserFacade {
    UUID createUser(CreateUserCommand command);
    boolean emailExists(String email);
    boolean usernameExists(String username);
    UserCredentialsResponse getUserCredentialsByEmail(String email);
}
