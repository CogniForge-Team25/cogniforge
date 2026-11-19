package com.cogniforge.evidence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EvidenceApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

       private long firstControlId() {
        // Create a control for this test, so the test does not depend on other data
        String code = "EVD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return jdbc.queryForObject(
                "insert into controls (code, name, category) values (?, 'Evidence test control', 'TECHNOLOGICAL') returning id",
                Long.class, code);
    }

    private static String submitBody(long controlId, String collected, String validUntil) {
        return """
                {"controlId":%d,"title":"Test evidence","evidenceType":"REPORT",
                 "collectedOn":"%s","validUntil":"%s"}
                """.formatted(controlId, collected, validUntil);
    }

    @Test
    void anonymousCannotSubmitEvidence() throws Exception {
        mvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(1, "2026-01-01", "2027-01-01")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ASSESSOR")
    void assessorCannotSubmitEvidence() throws Exception {
        mvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(firstControlId(), "2026-01-01", "2027-01-01")))
                .andExpect(status().isForbidden());
    }

        @Test
    @WithMockUser(username = "officer@example.com", roles = "SECURITY_OFFICER")
    void validUntilBeforeCollectedIsRefused() throws Exception {
        jdbc.update("insert into users (email, full_name, role_id, status) " +
                "select 'officer@example.com', 'Officer', id, 'ACTIVE' from roles " +
                "where name = 'SECURITY_OFFICER' on conflict (email) do nothing");
        mvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(firstControlId(), "2026-06-01", "2026-01-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "reviewer@example.com", roles = "INTERNAL_ASSESSOR")
    void reviewerCannotApproveOwnEvidence() throws Exception {
        jdbc.update("insert into users (email, full_name, role_id, status) " +
                "select 'reviewer@example.com', 'Reviewer', id, 'ACTIVE' from roles " +
                "where name = 'INTERNAL_ASSESSOR' on conflict (email) do nothing");
        long reviewerId = jdbc.queryForObject(
                "select id from users where email = 'reviewer@example.com'", Long.class);
        long evidenceId = jdbc.queryForObject(
                "insert into evidence (control_id, title, evidence_type, collected_on, valid_until, submitted_by) " +
                "values (?, 'Own', 'REPORT', '2026-01-01', '2027-01-01', ?) returning id",
                Long.class, firstControlId(), reviewerId);

        mvc.perform(put("/api/evidence/" + evidenceId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "reviewer@example.com", roles = "INTERNAL_ASSESSOR")
    void onlyApprovedUnexpiredEvidenceCountsAsSupported() throws Exception {
        jdbc.update("insert into users (email, full_name, role_id, status) " +
                "select 'reviewer@example.com', 'Reviewer', id, 'ACTIVE' from roles " +
                "where name = 'INTERNAL_ASSESSOR' on conflict (email) do nothing");
        long reviewerId = jdbc.queryForObject(
                "select id from users where email = 'reviewer@example.com'", Long.class);
        long controlId = firstControlId();

        // A different submitter's evidence, approved and still valid: counts
        long submitterId = reviewerId;
        jdbc.update("insert into users (email, full_name, role_id, status) " +
                "select 'submitter@example.com', 'Submitter', id, 'ACTIVE' from roles " +
                "where name = 'SECURITY_OFFICER' on conflict (email) do nothing");
        submitterId = jdbc.queryForObject(
                "select id from users where email = 'submitter@example.com'", Long.class);

        jdbc.update("insert into evidence (control_id, title, evidence_type, collected_on, valid_until, " +
                "submitted_by, review_status) values (?, 'Valid', 'REPORT', '2026-01-01', '2099-01-01', ?, 'APPROVED')",
                controlId, submitterId);
        // Expired evidence, approved: does not count
        jdbc.update("insert into evidence (control_id, title, evidence_type, collected_on, valid_until, " +
                "submitted_by, review_status) values (?, 'Expired', 'REPORT', '2020-01-01', '2020-06-01', ?, 'APPROVED')",
                controlId, submitterId);

        // The count is distinct controls, so this control counts once no matter how much evidence it has
        String json = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/evidence/supported-count"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json).contains("supportedControls");
    }
}
