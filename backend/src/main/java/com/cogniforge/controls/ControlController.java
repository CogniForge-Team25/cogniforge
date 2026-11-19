package com.cogniforge.controls;

import com.cogniforge.controls.ControlDtos.ControlView;
import com.cogniforge.controls.ControlDtos.CreateControlRequest;
import com.cogniforge.controls.ControlDtos.UpdateControlRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Reads: any signed-in user. Writes: Security Manager and Security Officer only.
// The check is enforced on the server by @PreAuthorize, not only in the UI.
@RestController
@RequestMapping("/api/controls")
public class ControlController {

    private static final String WRITERS = "hasAnyRole('SECURITY_MANAGER','SECURITY_OFFICER')";

    private final ControlService service;

    public ControlController(ControlService service) {
        this.service = service;
    }

    @GetMapping
    public List<ControlView> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ControlView get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(WRITERS)
    public ControlView create(@Valid @RequestBody CreateControlRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITERS)
    public ControlView update(@PathVariable long id, @Valid @RequestBody UpdateControlRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(WRITERS)
    public void delete(@PathVariable long id) {
        service.softDelete(id);
    }
}
