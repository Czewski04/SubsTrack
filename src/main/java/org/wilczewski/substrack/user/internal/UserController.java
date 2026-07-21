package org.wilczewski.substrack.user.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.wilczewski.substrack.user.api.dto.command.*;
import org.wilczewski.substrack.user.api.dto.request.*;
import org.wilczewski.substrack.user.api.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
class UserController {
    private final UserService userService;

//    @GetMapping
//    public ResponseEntity<List<UserResponse>> getAllUsers() {
//        List<UserResponse> userResponses = userService.getAllUsers();
//        return ResponseEntity.ok(userResponses);
//    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUserById(@AuthenticationPrincipal UUID id) {
        UserResponse userResponse = userService.getUserById(id);
        return ResponseEntity.ok(userResponse);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUserById(@AuthenticationPrincipal UUID id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/emails")
    public ResponseEntity<Void> addEmailToUser(@AuthenticationPrincipal UUID id, @RequestBody @Valid CreateUserEmailRequest request) {
        userService.createUserEmail(new CreateUserEmailCommand(id, request.email()));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/emails")
    public ResponseEntity<Void> updateUserEmail(@AuthenticationPrincipal UUID id, @RequestBody @Valid UpdateUserEmailRequest request) {
        userService.updateUserEmail(new UpdateUserEmailCommand(id, request.emailId(), request.newEmail()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/emails")
    public ResponseEntity<Void> deleteUserEmail(@AuthenticationPrincipal UUID id, @RequestBody @Valid DeleteUserEmailRequest request) {
        userService.deleteUserEmail(new DeleteUserEmailCommand(id, request.emailId()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> updateUserPassword(@AuthenticationPrincipal UUID id, @RequestBody @Valid UpdateUserPasswordRequest request) {
        userService.updateUserPassword(new UpdateUserPasswordCommand(id, request.password(), request.newPassword(), request.confirmNewPassword()));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/profile")
    public ResponseEntity<Void> updateUserProfile(@AuthenticationPrincipal UUID id, @RequestBody @Valid UpdateUserProfileRequest request) {
        userService.updateUserProfile(new UpdateUserProfileCommand(id, request.username()));
        return ResponseEntity.ok().build();
    }
}
