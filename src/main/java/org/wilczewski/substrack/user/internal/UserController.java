package org.wilczewski.substrack.user.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.wilczewski.substrack.common.exception.ErrorResponse;
import org.wilczewski.substrack.user.api.dto.command.*;
import org.wilczewski.substrack.user.api.dto.request.*;
import org.wilczewski.substrack.user.api.dto.response.UserResponse;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Current user profile and account management")
class UserController {
    private final UserService userService;

//    @GetMapping
//    public ResponseEntity<List<UserResponse>> getAllUsers() {
//        List<UserResponse> userResponses = userService.getAllUsers();
//        return ResponseEntity.ok(userResponses);
//    }

//  to do: createUser for admin

    @GetMapping("/me")
    @Operation(summary = "Get the current user profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getUserById(@Parameter(hidden = true) @AuthenticationPrincipal UUID id) {
        UserResponse userResponse = userService.getUserById(id);
        return ResponseEntity.ok(userResponse);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete the current user account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUserById(@Parameter(hidden = true) @AuthenticationPrincipal UUID id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/emails")
    @Operation(summary = "Add an email to the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Email added"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> addEmailToUser(@Parameter(hidden = true) @AuthenticationPrincipal UUID id, @RequestBody @Valid CreateUserEmailRequest request) {
        UUID emailId = userService.createUserEmail(new CreateUserEmailCommand(id, request.email()));
        return ResponseEntity.created(URI.create("/api/v1/users/me/emails/" + emailId)).build();
    }

    @PutMapping("/me/emails")
    @Operation(summary = "Update a user email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Email not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateUserEmail(@Parameter(hidden = true) @AuthenticationPrincipal UUID id, @RequestBody @Valid UpdateUserEmailRequest request) {
        userService.updateUserEmail(new UpdateUserEmailCommand(id, request.emailId(), request.newEmail()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/emails")
    @Operation(summary = "Delete a user email")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Email not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUserEmail(@Parameter(hidden = true) @AuthenticationPrincipal UUID id, @RequestBody @Valid DeleteUserEmailRequest request) {
        userService.deleteUserEmail(new DeleteUserEmailCommand(id, request.emailId()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change the current user password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateUserPassword(@Parameter(hidden = true) @AuthenticationPrincipal UUID id, @RequestBody @Valid UpdateUserPasswordRequest request) {
        userService.updateUserPassword(new UpdateUserPasswordCommand(id, request.password(), request.newPassword(), request.confirmNewPassword()));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update the current user profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateUserProfile(@Parameter(hidden = true) @AuthenticationPrincipal UUID id, @RequestBody @Valid UpdateUserProfileRequest request) {
        userService.updateUserProfile(new UpdateUserProfileCommand(id, request.username()));
        return ResponseEntity.ok().build();
    }
}
