package com.cogniforge.evidence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class EvidenceService {

    public record EvidenceView(long id, long controlId, String title, String evidenceType,
                               LocalDate collectedOn, LocalDate validUntil, String reviewStatus) {}

    private final JdbcTemplate jdbc;

    public EvidenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long submit(long controlId, String title, String type,
                       LocalDate collectedOn, LocalDate validUntil, long submittedBy) {
        if (validUntil.isBefore(collectedOn)) {
            throw new ResponseStatusException(BAD_REQUEST, "Valid-until date is before the collection date");
        }
        Integer control = jdbc.queryForObject(
                "select count(*) from controls where id = ? and deleted_at is null", Integer.class, controlId);
        if (control == null || control == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Control not found");
        }
        return jdbc.queryForObject(
                "insert into evidence (control_id, title, evidence_type, collected_on, valid_until, submitted_by) " +
                "values (?, ?, ?, ?, ?, ?) returning id",
                Long.class, controlId, title, type, collectedOn, validUntil, submittedBy);
    }

    // A reviewer approves or rejects. Approving your own evidence is refused.
    @Transactional
    public void review(long evidenceId, String decision, String comment, long reviewerId) {
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new ResponseStatusException(BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }
        Long submitter = jdbc.queryForObject(
                "select submitted_by from evidence where id = ?", Long.class, evidenceId);
        if (submitter == null) {
            throw new ResponseStatusException(NOT_FOUND, "Evidence not found");
        }
        if (submitter == reviewerId) {
            throw new ResponseStatusException(BAD_REQUEST, "You cannot review your own evidence");
        }
        jdbc.update(
                "update evidence set review_status = ?, reviewed_by = ?, review_comment = ? where id = ?",
                decision, reviewerId, comment, evidenceId);
    }

    public List<EvidenceView> forControl(long controlId) {
        return jdbc.query(
                "select id, control_id, title, evidence_type, collected_on, valid_until, review_status " +
                "from evidence where control_id = ? order by collected_on desc",
                (rs, n) -> new EvidenceView(
                        rs.getLong("id"), rs.getLong("control_id"), rs.getString("title"),
                        rs.getString("evidence_type"),
                        rs.getObject("collected_on", LocalDate.class),
                        rs.getObject("valid_until", LocalDate.class),
                        rs.getString("review_status")),
                controlId);
    }

    // Controls with at least one approved, unexpired evidence record
    public int supportedControlCount() {
        Integer n = jdbc.queryForObject(
                "select count(distinct c.id) from controls c " +
                "join evidence e on e.control_id = c.id " +
                "where c.deleted_at is null and e.review_status = 'APPROVED' and e.valid_until >= current_date",
                Integer.class);
        return n == null ? 0 : n;
    }
}
