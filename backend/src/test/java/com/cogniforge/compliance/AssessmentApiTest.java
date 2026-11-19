package com.cogniforge.compliance;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssessmentApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private long iso27001Id() {
        return jdbc.queryForObject("select id from frameworks where code = 'ISO27001'", Long.class);
    }

    private long createAssessment() throws Exception {
        String json = mvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test assessment\",\"frameworkId\":" + iso27001Id() + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long firstItem(long assessmentId) throws Exception {
        String json = mvc.perform(get("/api/assessments/" + assessmentId + "/checklist"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$[0].id")).longValue();
    }

    private int openGapsFor(long itemId) {
        return jdbc.queryForObject(
                "select count(*) from compliance_gaps where checklist_item_id = ? and status = 'OPEN'",
                Integer.class, itemId);
    }

    @Test
    void anonymousCannotCreateAssessment() throws Exception {
        mvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"frameworkId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void complianceOfficerCannotCreateAssessment() throws Exception {
        mvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"frameworkId\":" + iso27001Id() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXECUTIVE_REVIEWER")
    void executiveReviewerCannotRecordResult() throws Exception {
        mvc.perform(put("/api/assessments/1/checklist/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PASS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tester@example.com", roles = "INTERNAL_ASSESSOR")
    void failCreatesOneGapAndRepeatFailDoesNotDuplicate() throws Exception {
        // The test user must exist in users for the assessor id lookup
        jdbc.update("insert into users (email, full_name, role_id, status) " +
                "select 'tester@example.com', 'Tester', id, 'ACTIVE' from roles where name = 'INTERNAL_ASSESSOR' " +
                "on conflict (email) do nothing");

        long assessmentId = createAssessment();
        long itemId = firstItem(assessmentId);

        mvc.perform(put("/api/assessments/" + assessmentId + "/checklist/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"FAIL\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/assessments/" + assessmentId + "/checklist/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"FAIL\"}"))
                .andExpect(status().isOk());

        assertThat(openGapsFor(itemId)).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "tester@example.com", roles = "INTERNAL_ASSESSOR")
    void passResolvesTheGap() throws Exception {
        jdbc.update("insert into users (email, full_name, role_id, status) " +
                "select 'tester@example.com', 'Tester', id, 'ACTIVE' from roles where name = 'INTERNAL_ASSESSOR' " +
                "on conflict (email) do nothing");

        long assessmentId = createAssessment();
        long itemId = firstItem(assessmentId);

        mvc.perform(put("/api/assessments/" + assessmentId + "/checklist/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"FAIL\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/assessments/" + assessmentId + "/checklist/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PASS\"}"))
                .andExpect(status().isOk());

        assertThat(openGapsFor(itemId)).isEqualTo(0);
    }
}
