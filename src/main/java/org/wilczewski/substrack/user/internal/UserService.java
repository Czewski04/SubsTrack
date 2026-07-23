package org.wilczewski.substrack.user.internal;


import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wilczewski.substrack.user.api.UserFacade;
import org.wilczewski.substrack.user.api.dto.command.*;
import org.wilczewski.substrack.user.api.dto.response.UserCredentialsResponse;
import org.wilczewski.substrack.user.api.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
class UserService implements UserFacade {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UUID createUser(CreateUserCommand command) {
        User user = userMapper.toUser(command);
        user = userRepository.save(user);
        return user.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userByIdExists(UUID id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserCredentialsResponse getUserCredentialsByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User with email " + email + " does not exist");
        }
        return userMapper.toUserCredentialsResponse(user);
    }

    @Transactional
    public UUID createUserEmail(CreateUserEmailCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(user.getAdditionalEmails().stream().anyMatch(email -> email.getEmail().equals(command.email()))) {
            throw new IllegalArgumentException("Email already exists for this user");
        }
        user.addAdditionalEmail(command.email());
        userRepository.saveAndFlush(user);
        return user.getEmailIdByEmail(command.email());
        
    }

    @Transactional
    public void updateUserEmail(UpdateUserEmailCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if(user.getAdditionalEmails().stream().anyMatch(email -> email.getEmail().equals(command.newEmail()))) {
            throw new IllegalArgumentException("Email already exists for this user");
        }
        user.updateAdditionalEmail(command.emailId(), command.newEmail());
    }

    @Transactional
    public void updateUserPassword(UpdateUserPasswordCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if(!passwordEncoder.matches(command.password(), user.getPasswordHash())){
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if(!command.newPassword().equals(command.confirmNewPassword())){
            throw new IllegalArgumentException("Passwords do not match");
        }
        String newPasswordHash = passwordEncoder.encode(command.newPassword());
        user.setPasswordHash(newPasswordHash);
    }

    @Transactional
    public void updateUserProfile(UpdateUserProfileCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setUsername(command.username());
    }

    @Transactional
    public void deleteUserEmail(DeleteUserEmailCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try{
            user.deleteAdditionalEmail(command.emailId());
        } catch (Exception e){
            throw new IllegalArgumentException("Cannot delete primary email");
        }
    }

    @Transactional
    public void deleteUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toUserResponseList(users);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean userContainsEmail(UUID userId, UUID emailId) {
        if(!userRepository.existsById(userId)){
            return false;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getAdditionalEmails().stream().anyMatch(email -> email.getId().equals(emailId));
    }

    @Transactional(readOnly = true)
    @Override
    public String getUserEmailByEmailId(UUID emailId) {
        User user = userRepository.findByAdditionalEmailsId(emailId);
        if(user == null){
            throw new IllegalArgumentException("User not found");
        }
        return user.getAdditionalEmails().stream()
                .filter(email -> email.getId().equals(emailId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Email not found"))
                .getEmail();
    }
}

