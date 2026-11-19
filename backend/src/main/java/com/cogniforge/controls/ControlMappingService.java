package com.cogniforge.controls;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ControlMappingService {

    public record LinkedRequirement(long id, String clauseRef, String title) {}

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public ControlMappingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
    }

    public List<LinkedRequirement> list(long controlId) {
        requireControl(controlId);
        return jdbc.query("""
                select r.id, r.clause_ref, r.title
                from control_requirement cr
                join requirements r on r.id = cr.requirement_id
                where cr.control_id = ?
                order by string_to_array(substring(r.clause_ref from 3), '.')::int[]
                """,
                (rs, n) -> new LinkedRequirement(rs.getLong("id"), rs.getString("clause_ref"), rs.getString("title")),
                controlId);
    }

    // Replaces the whole mapping. An empty list clears it. Duplicates in the request are ignored.
    @Transactional
    public List<LinkedRequirement> replace(long controlId, List<Long> requirementIds) {
        requireControl(controlId);

        Set<Long> ids = new LinkedHashSet<>(requirementIds);
        if (!ids.isEmpty()) {
            Integer found = named.queryForObject(
                    "select count(*) from requirements where id in (:ids)",
                    new MapSqlParameterSource("ids", ids), Integer.class);
            if (found == null || found != ids.size()) {
                throw new ResponseStatusException(BAD_REQUEST, "One or more requirement IDs do not exist");
            }
        }

        jdbc.update("delete from control_requirement where control_id = ?", controlId);

        if (!ids.isEmpty()) {
            List<Object[]> rows = new ArrayList<>();
            for (Long requirementId : ids) {
                rows.add(new Object[] { controlId, requirementId });
            }
            jdbc.batchUpdate("insert into control_requirement (control_id, requirement_id) values (?, ?)", rows);
        }
        return list(controlId);
    }

    private void requireControl(long controlId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from controls where id = ? and deleted_at is null",
                Integer.class, controlId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Control not found");
        }
    }
}
