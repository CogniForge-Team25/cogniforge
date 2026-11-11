package com.cogniforge.auth;

import com.cogniforge.auth.AuthDtos.CreateUserRequest;
import com.cogniforge.auth.AuthDtos.InvitationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Access is enforced in SecurityConfig: IT_ADMINISTRATOR only.
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse create(@Valid @RequestBody CreateUserRequest request) {
        return authService.invite(request);
    }
}
