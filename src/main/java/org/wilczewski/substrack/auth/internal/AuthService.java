package org.wilczewski.substrack.auth.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wilczewski.substrack.auth.api.AuthFacade;
import org.wilczewski.substrack.auth.api.dto.command.LoginCommand;
import org.wilczewski.substrack.auth.api.dto.command.RegisterCommand;
import org.wilczewski.substrack.auth.api.dto.response.TokenResponse;
import org.wilczewski.substrack.common.exception.BadRequestException;
import org.wilczewski.substrack.common.exception.ConflictException;
import org.wilczewski.substrack.common.exception.UnauthorizedException;
import org.wilczewski.substrack.user.api.UserFacade;
import org.wilczewski.substrack.user.api.dto.command.CreateUserCommand;
import org.wilczewski.substrack.user.api.dto.response.UserCredentialsResponse;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
class AuthService implements AuthFacade {
    private final UserFacade userFacade;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public TokenResponse register(RegisterCommand command) {
        if (userFacade.emailExists(command.email()) || userFacade.usernameExists(command.username())) {
            throw new ConflictException("Username or email already exists");
        }

        if (!command.password().equals(command.confirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }

        String passwordHash = passwordEncoder.encode(command.password());
        CreateUserCommand createUserCommand = new CreateUserCommand(
                command.username(),
                command.email(),
                passwordHash
        );
        UUID uuid = userFacade.createUser(createUserCommand);
        String token = jwtService.generateToken(uuid);
        return new TokenResponse(token);
    }

    public TokenResponse login(LoginCommand command) {
        if (!userFacade.emailExists(command.email())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        UserCredentialsResponse userCredentials = userFacade.getUserCredentialsByEmail(command.email());
        if (!passwordEncoder.matches(command.password(), userCredentials.passwordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = jwtService.generateToken(userCredentials.id());
        return new TokenResponse(token);
    }
}