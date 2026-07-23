package org.wilczewski.substrack.user.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.wilczewski.substrack.common.exception.BadRequestException;
import org.wilczewski.substrack.common.exception.ConflictException;
import org.wilczewski.substrack.common.exception.ResourceNotFoundException;
import org.wilczewski.substrack.user.api.dto.command.CreateUserCommand;
import org.wilczewski.substrack.user.api.dto.command.CreateUserEmailCommand;
import org.wilczewski.substrack.user.api.dto.command.DeleteUserEmailCommand;
import org.wilczewski.substrack.user.api.dto.command.UpdateUserEmailCommand;
import org.wilczewski.substrack.user.api.dto.command.UpdateUserPasswordCommand;
import org.wilczewski.substrack.user.api.dto.command.UpdateUserProfileCommand;
import org.wilczewski.substrack.user.api.dto.response.UserCredentialsResponse;
import org.wilczewski.substrack.user.api.dto.response.UserResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void createUserMapsSavesAndReturnsId() {
        CreateUserCommand command = new CreateUserCommand("alice", "alice@example.com", "hash");
        User mappedUser = user(UUID.randomUUID(), "alice", "alice@example.com", "hash");
        User savedUser = user(UUID.randomUUID(), "alice", "alice@example.com", "hash");
        when(userMapper.toUser(command)).thenReturn(mappedUser);
        when(userRepository.save(mappedUser)).thenReturn(savedUser);

        UUID result = userService.createUser(command);

        assertEquals(savedUser.getId(), result);
        verify(userMapper).toUser(command);
        verify(userRepository).save(mappedUser);
    }

    @Test
    void existenceMethodsDelegateToRepository() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsById(userId)).thenReturn(true);

        assertTrue(userService.emailExists("alice@example.com"));
        assertFalse(userService.usernameExists("alice"));
        assertTrue(userService.userByIdExists(userId));
    }

    @Test
    void getUserCredentialsByEmailMapsExistingUser() {
        User user = user(UUID.randomUUID(), "alice", "alice@example.com", "hash");
        UserCredentialsResponse response =
                new UserCredentialsResponse(user.getId(), user.getEmail(), user.getPasswordHash());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        when(userMapper.toUserCredentialsResponse(user)).thenReturn(response);

        assertSame(response, userService.getUserCredentialsByEmail(user.getEmail()));
    }

    @Test
    void getUserCredentialsByEmailRejectsMissingUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserCredentialsByEmail("missing@example.com"));
        verifyNoInteractions(userMapper);
    }

    @Test
    void createUserEmailAddsEmailPersistsAndReturnsGeneratedEmailId() {
        UUID userId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        User user = user(userId, "alice", "alice@example.com", "hash");
        CreateUserEmailCommand command = new CreateUserEmailCommand(userId, "billing@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenAnswer(invocation -> {
            user.getAdditionalEmails().getLast().setId(emailId);
            return user;
        });

        UUID result = userService.createUserEmail(command);

        assertEquals(emailId, result);
        assertTrue(user.getAdditionalEmails().stream()
                .anyMatch(email -> email.getEmail().equals(command.email())));
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void createUserEmailRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.createUserEmail(new CreateUserEmailCommand(userId, "new@example.com")));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createUserEmailRejectsDuplicateForUser() {
        UUID userId = UUID.randomUUID();
        User user = userWithAdditionalEmail(userId, UUID.randomUUID(), "billing@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class,
                () -> userService.createUserEmail(new CreateUserEmailCommand(userId, "billing@example.com")));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateUserEmailChangesRequestedEmail() {
        UUID userId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        User user = userWithAdditionalEmail(userId, emailId, "old@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateUserEmail(new UpdateUserEmailCommand(userId, emailId, "new@example.com"));

        assertEquals("new@example.com", user.getAdditionalEmails().getFirst().getEmail());
    }

    @Test
    void updateUserEmailRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserEmail(
                new UpdateUserEmailCommand(userId, UUID.randomUUID(), "new@example.com")));
    }

    @Test
    void updateUserEmailRejectsDuplicateForUser() {
        UUID userId = UUID.randomUUID();
        User user = userWithAdditionalEmail(userId, UUID.randomUUID(), "taken@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () -> userService.updateUserEmail(
                new UpdateUserEmailCommand(userId, UUID.randomUUID(), "taken@example.com")));
    }

    @Test
    void updateUserPasswordEncodesAndStoresNewPassword() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "alice", "alice@example.com", "old-hash");
        UpdateUserPasswordCommand command =
                new UpdateUserPasswordCommand(userId, "old-password", "new-password", "new-password");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(command.password(), user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(command.newPassword())).thenReturn("new-hash");

        userService.updateUserPassword(command);

        assertEquals("new-hash", user.getPasswordHash());
    }

    @Test
    void updateUserPasswordRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserPassword(
                new UpdateUserPasswordCommand(userId, "old", "new", "new")));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUserPasswordRejectsIncorrectCurrentPassword() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "alice", "alice@example.com", "old-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.updateUserPassword(
                new UpdateUserPasswordCommand(userId, "wrong", "new", "new")));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUserPasswordRejectsMismatchedNewPasswords() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "alice", "alice@example.com", "old-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.updateUserPassword(
                new UpdateUserPasswordCommand(userId, "old", "new", "different")));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUserProfileChangesUsername() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "old-name", "alice@example.com", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateUserProfile(new UpdateUserProfileCommand(userId, "new-name"));

        assertEquals("new-name", user.getUsername());
    }

    @Test
    void updateUserProfileRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserProfile(new UpdateUserProfileCommand(userId, "new-name")));
    }

    @Test
    void deleteUserEmailRemovesRequestedNonPrimaryEmail() {
        UUID userId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        User user = userWithAdditionalEmail(userId, emailId, "other@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUserEmail(new DeleteUserEmailCommand(userId, emailId));

        assertTrue(user.getAdditionalEmails().isEmpty());
    }

    @Test
    void deleteUserEmailRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUserEmail(new DeleteUserEmailCommand(userId, UUID.randomUUID())));
    }

    @Test
    void deleteUserEmailRejectsPrimaryEmail() {
        UUID userId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        User user = userWithAdditionalEmail(userId, emailId, "alice@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class,
                () -> userService.deleteUserEmail(new DeleteUserEmailCommand(userId, emailId)));
    }

    @Test
    void deleteUserEmailRejectsUnknownEmail() {
        UUID userId = UUID.randomUUID();
        User user = userWithAdditionalEmail(userId, UUID.randomUUID(), "other@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUserEmail(new DeleteUserEmailCommand(userId, UUID.randomUUID())));
    }

    @Test
    void deleteUserByIdDeletesExistingUser() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "alice", "alice@example.com", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUserById(userId);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserByIdRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUserById(userId));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void getUserByIdMapsExistingUser() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "alice", "alice@example.com", "hash");
        UserResponse response = new UserResponse("alice", "alice@example.com", List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        assertSame(response, userService.getUserById(userId));
    }

    @Test
    void getUserByIdRejectsMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(userId));
        verifyNoInteractions(userMapper);
    }

    @Test
    void getAllUsersMapsRepositoryResult() {
        List<User> users = List.of(user(UUID.randomUUID(), "alice", "alice@example.com", "hash"));
        List<UserResponse> responses = List.of(new UserResponse("alice", "alice@example.com", List.of()));
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toUserResponseList(users)).thenReturn(responses);

        assertSame(responses, userService.getAllUsers());
    }

    @Test
    void userContainsEmailReturnsFalseWithoutLoadingMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertFalse(userService.userContainsEmail(userId, UUID.randomUUID()));
        verify(userRepository, never()).findById(userId);
    }

    @Test
    void userContainsEmailReportsWhetherEmailBelongsToUser() {
        UUID userId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        User user = userWithAdditionalEmail(userId, emailId, "other@example.com");
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertTrue(userService.userContainsEmail(userId, emailId));
        assertFalse(userService.userContainsEmail(userId, UUID.randomUUID()));
    }

    @Test
    void userContainsEmailRejectsUserRemovedBetweenRepositoryCalls() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.userContainsEmail(userId, UUID.randomUUID()));
    }

    @Test
    void getUserEmailByEmailIdReturnsMatchingAddress() {
        UUID emailId = UUID.randomUUID();
        User user = userWithAdditionalEmail(UUID.randomUUID(), emailId, "other@example.com");
        when(userRepository.findByAdditionalEmailsId(emailId)).thenReturn(user);

        assertEquals("other@example.com", userService.getUserEmailByEmailId(emailId));
    }

    @Test
    void getUserEmailByEmailIdRejectsMissingOwningUser() {
        UUID emailId = UUID.randomUUID();
        when(userRepository.findByAdditionalEmailsId(emailId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserEmailByEmailId(emailId));
    }

    @Test
    void getUserEmailByEmailIdRejectsInconsistentRepositoryResult() {
        UUID emailId = UUID.randomUUID();
        User user = userWithAdditionalEmail(UUID.randomUUID(), UUID.randomUUID(), "other@example.com");
        when(userRepository.findByAdditionalEmailsId(emailId)).thenReturn(user);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserEmailByEmailId(emailId));
    }

    private static User user(UUID id, String username, String email, String passwordHash) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        return user;
    }

    private static User userWithAdditionalEmail(UUID userId, UUID emailId, String additionalEmail) {
        User user = user(userId, "alice", "alice@example.com", "hash");
        user.addAdditionalEmail(additionalEmail);
        user.getAdditionalEmails().getFirst().setId(emailId);
        return user;
    }
}
