package org.wilczewski.substrack.auth.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserFacade userFacade;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerReturnsTokenWhenCommandIsValid() {
        RegisterCommand command = new RegisterCommand(
                "subscriber", "subscriber@example.com", "secret", "secret");
        UUID userId = UUID.randomUUID();

        when(passwordEncoder.encode("secret")).thenReturn("password-hash");
        when(userFacade.createUser(new CreateUserCommand(
                "subscriber", "subscriber@example.com", "password-hash"))).thenReturn(userId);
        when(jwtService.generateToken(userId)).thenReturn("jwt-token");

        TokenResponse response = authService.register(command);

        assertEquals("jwt-token", response.jwtToken());
        verify(userFacade).emailExists("subscriber@example.com");
        verify(userFacade).usernameExists("subscriber");
        verify(passwordEncoder).encode("secret");
        verify(userFacade).createUser(new CreateUserCommand(
                "subscriber", "subscriber@example.com", "password-hash"));
        verify(jwtService).generateToken(userId);
    }

    @Test
    void registerThrowsConflictWhenEmailAlreadyExists() {
        RegisterCommand command = new RegisterCommand(
                "subscriber", "subscriber@example.com", "secret", "secret");
        when(userFacade.emailExists("subscriber@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(command));

        verify(userFacade).emailExists("subscriber@example.com");
        verify(userFacade, never()).usernameExists(command.username());
        verifyNoInteractions(passwordEncoder, jwtService);
        verify(userFacade, never()).createUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerThrowsConflictWhenUsernameAlreadyExists() {
        RegisterCommand command = new RegisterCommand(
                "subscriber", "subscriber@example.com", "secret", "secret");
        when(userFacade.usernameExists("subscriber")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(command));

        verify(userFacade).emailExists("subscriber@example.com");
        verify(userFacade).usernameExists("subscriber");
        verifyNoInteractions(passwordEncoder, jwtService);
        verify(userFacade, never()).createUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerThrowsBadRequestWhenPasswordsDoNotMatch() {
        RegisterCommand command = new RegisterCommand(
                "subscriber", "subscriber@example.com", "secret", "different");

        assertThrows(BadRequestException.class, () -> authService.register(command));

        verify(userFacade).emailExists("subscriber@example.com");
        verify(userFacade).usernameExists("subscriber");
        verifyNoInteractions(passwordEncoder, jwtService);
        verify(userFacade, never()).createUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        LoginCommand command = new LoginCommand("subscriber@example.com", "secret");
        UUID userId = UUID.randomUUID();
        UserCredentialsResponse credentials =
                new UserCredentialsResponse(userId, command.email(), "password-hash");
        when(userFacade.emailExists(command.email())).thenReturn(true);
        when(userFacade.getUserCredentialsByEmail(command.email())).thenReturn(credentials);
        when(passwordEncoder.matches(command.password(), credentials.passwordHash())).thenReturn(true);
        when(jwtService.generateToken(userId)).thenReturn("jwt-token");

        TokenResponse response = authService.login(command);

        assertEquals("jwt-token", response.jwtToken());
        verify(userFacade).getUserCredentialsByEmail(command.email());
        verify(passwordEncoder).matches(command.password(), credentials.passwordHash());
        verify(jwtService).generateToken(userId);
    }

    @Test
    void loginThrowsUnauthorizedWhenEmailDoesNotExist() {
        LoginCommand command = new LoginCommand("missing@example.com", "secret");

        assertThrows(UnauthorizedException.class, () -> authService.login(command));

        verify(userFacade).emailExists(command.email());
        verify(userFacade, never()).getUserCredentialsByEmail(command.email());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void loginThrowsUnauthorizedWhenPasswordIsInvalid() {
        LoginCommand command = new LoginCommand("subscriber@example.com", "wrong");
        UserCredentialsResponse credentials =
                new UserCredentialsResponse(UUID.randomUUID(), command.email(), "password-hash");
        when(userFacade.emailExists(command.email())).thenReturn(true);
        when(userFacade.getUserCredentialsByEmail(command.email())).thenReturn(credentials);
        when(passwordEncoder.matches(command.password(), credentials.passwordHash())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(command));

        verify(passwordEncoder).matches(command.password(), credentials.passwordHash());
        verifyNoInteractions(jwtService);
    }
}
