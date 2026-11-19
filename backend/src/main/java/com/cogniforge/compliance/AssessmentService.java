package com.cogniforge.compliance;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AssessmentService {

    public record ChecklistItemView(long id, long requirementId, String clauseRef, String result) {}
    public record AssessmentView(long id, String title, long frameworkId) {}
    public record GapView(long id, long checklistItemId, String description, String status) {}

    private final JdbcTemplate jdbc;

    public AssessmentService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Creates an assessment with one checklist item per requirement of the framework.
    @Transactional
    public long create(String title, long frameworkId, long createdBy) {
        Integer exists = jdbc.queryForObject(
                "select count(*) from frameworks where id = ?", Integer.class, frameworkId);
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Framework does not exist");
        }
        Long id = jdbc.queryForObject(
                "insert into assessments (title, framework_id, created_by) values (?, ?, ?) returning id",
                Long.class, title, frameworkId, createdBy);
        jdbc.update(
                "insert into checklist_items (assessment_id, requirement_id) " +
                "select ?, id from requirements where framework_id = ?",
                id, frameworkId);
        return id;
    }

    public List<ChecklistItemView> checklist(long assessmentId) {
        requireAssessment(assessmentId);
        return jdbc.query("""
                select ci.id, ci.requirement_id, r.clause_ref, coalesce(ci.result, 'NOT_ASSESSED') as result
                from checklist_items ci
                join requirements r on r.id = ci.requirement_id
                where ci.assessment_id = ?
                order by string_to_array(substring(r.clause_ref from 3), '.')::int[]
                """,
                (rs, n) -> new ChecklistItemView(
                        rs.getLong("id"), rs.getLong("requirement_id"),
                        rs.getString("clause_ref"), rs.getString("result")),
                assessmentId);
    }

    // Records a result. A FAIL opens exactly one gap; a later PASS resolves it.
    @Transactional
    public void record(long assessmentId, long checklistItemId, String result, long assessedBy) {
        if (!"PASS".equals(result) && !"FAIL".equals(result)) {
            throw new ResponseStatusException(BAD_REQUEST, "Result must be PASS or FAIL");
        }
        int rows = jdbc.update(
                "update checklist_items set result = ?, assessed_at = now(), assessed_by = ? " +
                "where id = ? and assessment_id = ?",
                result, assessedBy, checklistItemId, assessmentId);
        if (rows == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Checklist item not found in this assessment");
        }

        if ("FAIL".equals(result)) {
            try {
                jdbc.update(
                        "insert into compliance_gaps (checklist_item_id, description) values (?, ?)",
                        checklistItemId, "Requirement not met (checklist item " + checklistItemId + ")");
            } catch (DuplicateKeyException e) {
                // Already has an open gap for this item; nothing to add
            }
        } else {
            jdbc.update(
                    "update compliance_gaps set status = 'RESOLVED' " +
                    "where checklist_item_id = ? and status = 'OPEN'",
                    checklistItemId);
        }
    }

    public List<GapView> openGaps() {
        return jdbc.query(
                "select id, checklist_item_id, description, status from compliance_gaps where status = 'OPEN' order by id",
                (rs, n) -> new GapView(rs.getLong("id"), rs.getLong("checklist_item_id"),
                        rs.getString("description"), rs.getString("status")));
    }

    private void requireAssessment(long assessmentId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from assessments where id = ?", Integer.class, assessmentId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Assessment not found");
        }
    }
}
