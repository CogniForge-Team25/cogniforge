package com.cogniforge.controls;

import com.cogniforge.controls.ControlDtos.ControlView;
import com.cogniforge.controls.ControlDtos.CreateControlRequest;
import com.cogniforge.controls.ControlDtos.UpdateControlRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ControlService {

    private static final String SELECT =
            "select id, code, name, category, status, owner_id from controls ";


    private final JdbcTemplate jdbc;

    public ControlService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ControlView> list() {
        return jdbc.query(SELECT + "where deleted_at is null order by code", this::map);
    }

    public ControlView get(long id) {
        List<ControlView> found = jdbc.query(SELECT + "where id = ? and deleted_at is null", this::map, id);
        if (found.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Control not found");
        }
        return found.get(0);
    }

    @Transactional
    public ControlView create(CreateControlRequest r) {
        String status = r.status() == null ? "PLANNED" : r.status();
        try {
            Long id = jdbc.queryForObject(
                    "insert into controls (code, name, category, status, owner_id) " +
                    "values (?, ?, ?, ?, ?) returning id",
                    Long.class, r.code(), r.name(), r.category(), status, r.ownerId());
            return get(id);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(CONFLICT, "A control with this code already exists");
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Owner does not exist");
        }
    }

    @Transactional
    public ControlView update(long id, UpdateControlRequest r) {
        int rows;
        try {
            rows = jdbc.update(
                    "update controls set name = ?, category = ?, status = ?, owner_id = ? " +
                    "where id = ? and deleted_at is null",
                    r.name(), r.category(), r.status(), r.ownerId(), id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Owner does not exist");
        }
        if (rows == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Control not found");
        }
        return get(id);
    }

    // Soft delete keeps the row, so past assessments that reference the control stay valid
    @Transactional
    public void softDelete(long id) {
        int rows = jdbc.update(
                "update controls set deleted_at = now() where id = ? and deleted_at is null", id);
        if (rows == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Control not found");
        }
    }

    private ControlView map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        long owner = rs.getLong("owner_id");
        return new ControlView(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("status"),
                rs.wasNull() ? null : owner);
    }
}
