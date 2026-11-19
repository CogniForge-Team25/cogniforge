package com.cogniforge.controls;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControlApiTest {

    @Autowired
    MockMvc mvc;

    // Unique per run, so repeated test runs never collide on the unique code
    private static String newCode() {
        return "TST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String body(String code) {
        return """
                {"code":"%s","name":"Test control","category":"TECHNOLOGICAL"}
                """.formatted(code);
    }

    @Test
    void anonymousCannotListControls() throws Exception {
        mvc.perform(get("/api/controls")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotCreateControl() throws Exception {
        mvc.perform(post("/api/controls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(newCode())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ASSESSOR")
    void assessorCannotCreateControl() throws Exception {
        mvc.perform(post("/api/controls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(newCode())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SECURITY_OFFICER")
    void officerCreatesControlThenDuplicateCodeIsRefused() throws Exception {
        String code = newCode();
        mvc.perform(post("/api/controls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(code)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/controls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(code)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "SECURITY_OFFICER")
    void invalidCategoryIsRejectedBeforeReachingTheDatabase() throws Exception {
        mvc.perform(post("/api/controls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"X","category":"NOT_A_CATEGORY"}
                                """.formatted(newCode())))
                .andExpect(status().isBadRequest());
    }
}
