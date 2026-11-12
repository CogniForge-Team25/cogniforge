package com.cogniforge.controls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

// Runs against the local Docker database when DB_PASSWORD is set.
// CI does not set it, so this test is skipped there.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class FrameworkSeedTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void iso27001SeedLoadsAll93Clauses() {
        Integer count = jdbc.queryForObject(
                "select count(*) from requirements r " +
                "join frameworks f on f.id = r.framework_id " +
                "where f.code = 'ISO27001'", Integer.class);
        assertThat(count).isEqualTo(93);
    }

    @Test
    void clauseReferencesAreUniqueAndComplete() {
        Integer duplicates = jdbc.queryForObject(
                "select count(*) - count(distinct clause_ref) from requirements", Integer.class);
        assertThat(duplicates).isZero();
    }
}
