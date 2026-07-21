package org.wilczewski.substrack.auth.internal;

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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class AuthController {
    private final AuthService authService;
    private final AuthMapper authMapper;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegisterCommand command = authMapper.toRegisterCommand(request);
        TokenResponse tokenResponse = authService.register(command);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginCommand command = authMapper.toLoginCommand(request);
        TokenResponse tokenResponse = authService.login(command);
        return ResponseEntity.ok(tokenResponse);
    }
}
