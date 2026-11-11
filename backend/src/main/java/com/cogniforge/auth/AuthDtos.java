package com.cogniforge.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 128) String password) {}

    public record LoginResponse(String accessToken, String tokenType, String role) {}

    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 50) String role) {}

    // Returned to the administrator once. They share the link privately.
    public record InvitationResponse(String email, String invitationToken, Instant expiresAt) {}

    public record ActivateRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 12, max = 128) String password) {}
}
