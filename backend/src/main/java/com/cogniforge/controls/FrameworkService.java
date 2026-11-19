package com.cogniforge.controls;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FrameworkService {

    public record FrameworkSummary(String code, String name, String version, long clauseCount) {}
    public record RequirementView(String clauseRef, String title) {}

    private final JdbcTemplate jdbc;

    public FrameworkService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<FrameworkSummary> listFrameworks() {
        return jdbc.query("""
                select f.code, f.name, f.version, count(r.id) as clause_count
                from frameworks f
                left join requirements r on r.framework_id = f.id
                group by f.id, f.code, f.name, f.version
                order by f.code
                """,
                (rs, n) -> new FrameworkSummary(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("version"),
                        rs.getLong("clause_count")));
    }

    // Sorts clause references numerically (A.5.2 before A.5.10), not as text
    public List<RequirementView> listRequirements(String code) {
        return jdbc.query("""
                select r.clause_ref, r.title
                from requirements r
                join frameworks f on f.id = r.framework_id
                where f.code = ?
                order by string_to_array(substring(r.clause_ref from 3), '.')::int[]
                """,
                (rs, n) -> new RequirementView(rs.getString("clause_ref"), rs.getString("title")),
                code);
    }
}
