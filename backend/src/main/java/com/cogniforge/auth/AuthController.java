package com.cogniforge.auth;

import com.cogniforge.auth.AuthDtos.ActivateRequest;
import com.cogniforge.auth.AuthDtos.LoginRequest;
import com.cogniforge.auth.AuthDtos.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@Valid @RequestBody ActivateRequest request) {
        authService.activate(request.token(), request.password());
    }
}
