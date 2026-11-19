package com.cogniforge.evidence;

import com.cogniforge.evidence.EvidenceService.EvidenceView;
import com.cogniforge.compliance.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    public record SubmitEvidenceRequest(
            @NotNull Long controlId,
            @NotBlank @Size(max = 150) String title,
            @NotBlank String evidenceType,
            @NotNull LocalDate collectedOn,
            @NotNull LocalDate validUntil) {}

    public record ReviewRequest(
            @NotBlank String decision,
            @Size(max = 255) String comment) {}

    private final EvidenceService service;
    private final CurrentUser currentUser;

    public EvidenceController(EvidenceService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SECURITY_MANAGER','SECURITY_OFFICER')")
    public Map<String, Long> submit(@Valid @RequestBody SubmitEvidenceRequest r) {
        long id = service.submit(r.controlId(), r.title(), r.evidenceType(),
                r.collectedOn(), r.validUntil(), currentUser.id());
        return Map.of("id", id);
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('INTERNAL_ASSESSOR','SECURITY_MANAGER')")
    public void review(@PathVariable long id, @Valid @RequestBody ReviewRequest r) {
        service.review(id, r.decision(), r.comment(), currentUser.id());
    }

    @GetMapping("/control/{controlId}")
    public List<EvidenceView> forControl(@PathVariable long controlId) {
        return service.forControl(controlId);
    }

    @GetMapping("/supported-count")
    public Map<String, Integer> supportedCount() {
        return Map.of("supportedControls", service.supportedControlCount());
    }
}
