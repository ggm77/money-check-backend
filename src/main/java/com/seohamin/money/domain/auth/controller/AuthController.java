package com.seohamin.money.domain.auth.controller;

import com.seohamin.money.domain.auth.dto.AuthTokenResponse;
import com.seohamin.money.domain.auth.dto.LoginRequest;
import com.seohamin.money.domain.auth.dto.RefreshTokenRequest;
import com.seohamin.money.domain.auth.dto.SignupRequest;
import com.seohamin.money.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthTokenResponse> signup(@Valid @RequestBody final SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody final LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(
            @Valid @RequestBody final RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
