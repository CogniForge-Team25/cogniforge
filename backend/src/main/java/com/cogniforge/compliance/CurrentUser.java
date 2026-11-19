package com.cogniforge.compliance;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

// Resolves the signed-in user's database id from the JWT subject (their email).
@Component
public class CurrentUser {

    private final JdbcTemplate jdbc;

    public CurrentUser(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }
        Long id = jdbc.queryForObject(
                "select id from users where email = ?", Long.class, auth.getName());
        if (id == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }
        return id;
    }
}
