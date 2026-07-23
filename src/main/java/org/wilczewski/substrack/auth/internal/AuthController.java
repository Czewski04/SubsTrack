package org.wilczewski.substrack.auth.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wilczewski.substrack.auth.api.dto.command.LoginCommand;
import org.wilczewski.substrack.auth.api.dto.command.RegisterCommand;
import org.wilczewski.substrack.auth.api.dto.request.LoginRequest;
import org.wilczewski.substrack.auth.api.dto.request.RegisterRequest;
import org.wilczewski.substrack.auth.api.dto.response.TokenResponse;
import org.wilczewski.substrack.common.exception.ErrorResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration and login")
class AuthController {
    private final AuthService authService;
    private final AuthMapper authMapper;

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegisterCommand command = authMapper.toRegisterCommand(request);
        TokenResponse tokenResponse = authService.register(command);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in with credentials")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginCommand command = authMapper.toLoginCommand(request);
        TokenResponse tokenResponse = authService.login(command);
        return ResponseEntity.ok(tokenResponse);
    }
}
