package com.cogniforge.auth;

import com.cogniforge.auth.AuthDtos.CreateUserRequest;
import com.cogniforge.auth.AuthDtos.InvitationResponse;
import com.cogniforge.auth.AuthDtos.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import static org.springframework.http.HttpStatus.*;

@Service
public class AuthService {

    private static final Duration INVITE_TTL = Duration.ofHours(72);
    private static final String GENERIC_LOGIN_ERROR = "Invalid email or password";

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;

    public AuthService(UserRepository users, RoleRepository roles,
                       PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.jwtService = jwtService;
        // Used when the email is unknown, so unknown and wrong-password cases take similar time.
        this.dummyHash = encoder.encode(Base64.getEncoder().encodeToString(random.generateSeed(16)));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email).orElse(null);
        String hashToCheck = (user != null && user.getPasswordHash() != null)
                ? user.getPasswordHash()
                : dummyHash;
        boolean passwordMatches = encoder.matches(password, hashToCheck);

        if (user == null || user.getStatus() != UserStatus.ACTIVE || !passwordMatches) {
            throw new ResponseStatusException(UNAUTHORIZED, GENERIC_LOGIN_ERROR);
        }
        return new LoginResponse(jwtService.issue(user), "Bearer", user.getRole().getName());
    }

    @Transactional
    public InvitationResponse invite(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "User already exists");
        }
        Role role = roles.findByName(request.role())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unknown role"));

        String token = newToken();
        Instant expiresAt = Instant.now().plus(INVITE_TTL);

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(role);
        user.setStatus(UserStatus.INVITED);
        user.setInvitationTokenHash(sha256(token));
        user.setInvitationExpiresAt(expiresAt);
        users.save(user);

        return new InvitationResponse(email, token, expiresAt);
    }

    @Transactional
    public void activate(String token, String password) {
        ResponseStatusException invalid =
                new ResponseStatusException(BAD_REQUEST, "Invitation is invalid or has expired");

        User user = users.findByInvitationTokenHash(sha256(token)).orElseThrow(() -> invalid);
        if (user.getStatus() != UserStatus.INVITED
                || user.getInvitationExpiresAt() == null
                || user.getInvitationExpiresAt().isBefore(Instant.now())) {
            throw invalid;
        }

        user.setPasswordHash(encoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setInvitationTokenHash(null);   // single use
        user.setInvitationExpiresAt(null);
    }

    private String newToken() {
        byte[] bytes = new byte[32];   // 256 bits of randomness
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
