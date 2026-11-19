package com.cogniforge.controls;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControlMappingApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private static String newCode() {
        return "MAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private long createControl() throws Exception {
        String json = mvc.perform(post("/api/controls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Mapped control","category":"TECHNOLOGICAL"}
                                """.formatted(newCode())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private List<Long> twoRequirementIds() {
        return jdbc.queryForList(
                "select r.id from requirements r join frameworks f on f.id = r.framework_id " +
                "where f.code = 'ISO27001' order by r.clause_ref limit 2", Long.class);
    }

    private static String mapping(long a, long b) {
        return """
                {"requirementIds":[%d,%d]}
                """.formatted(a, b);
    }

    @Test
    void anonymousCannotChangeMapping() throws Exception {
        mvc.perform(put("/api/controls/1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementIds\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ASSESSOR")
    void assessorCannotChangeMapping() throws Exception {
        mvc.perform(put("/api/controls/1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SECURITY_OFFICER")
    void officerReplacesMappingThenClearsIt() throws Exception {
        long controlId = createControl();
        List<Long> ids = twoRequirementIds();

        mvc.perform(put("/api/controls/" + controlId + "/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapping(ids.get(0), ids.get(1))))
                .andExpect(status().isOk());

        String linked = mvc.perform(get("/api/controls/" + controlId + "/requirements"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat((Integer) JsonPath.read(linked, "$.length()")).isEqualTo(2);

        mvc.perform(put("/api/controls/" + controlId + "/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementIds\":[]}"))
                .andExpect(status().isOk());

        String cleared = mvc.perform(get("/api/controls/" + controlId + "/requirements"))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat((Integer) JsonPath.read(cleared, "$.length()")).isEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "SECURITY_OFFICER")
    void unknownControlIsNotFound() throws Exception {
        List<Long> ids = twoRequirementIds();
        mvc.perform(put("/api/controls/999999999/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapping(ids.get(0), ids.get(1))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SECURITY_OFFICER")
    void unknownRequirementIsRefused() throws Exception {
        long controlId = createControl();
        mvc.perform(put("/api/controls/" + controlId + "/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementIds\":[999999999]}"))
                .andExpect(status().isBadRequest());
    }
}
