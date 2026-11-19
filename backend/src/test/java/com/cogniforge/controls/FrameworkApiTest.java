package com.cogniforge.controls;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FrameworkApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonymousRequestToFrameworksIsRefused() throws Exception {
        mvc.perform(get("/api/frameworks")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousRequestToRequirementsIsRefused() throws Exception {
        mvc.perform(get("/api/frameworks/ISO27001/requirements")).andExpect(status().isUnauthorized());
    }
}
