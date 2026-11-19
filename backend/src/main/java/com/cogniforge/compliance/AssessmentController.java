package com.cogniforge.compliance;

import com.cogniforge.compliance.AssessmentService.ChecklistItemView;
import com.cogniforge.compliance.AssessmentService.GapView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Reads: any signed-in user. Writes: Security Manager, Security Officer, Internal Assessor.
@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private static final String WRITERS =
            "hasAnyRole('SECURITY_MANAGER','SECURITY_OFFICER','INTERNAL_ASSESSOR')";

    public record CreateAssessmentRequest(
            @NotBlank @Size(max = 150) String title,
            @NotNull Long frameworkId) {}

    public record RecordResultRequest(@NotBlank String result) {}

    private final AssessmentService service;
    private final CurrentUser currentUser;

    public AssessmentController(AssessmentService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(WRITERS)
    public Map<String, Long> create(@Valid @RequestBody CreateAssessmentRequest request) {
        long id = service.create(request.title(), request.frameworkId(), currentUser.id());
        return Map.of("id", id);
    }

    @GetMapping("/{id}/checklist")
    public List<ChecklistItemView> checklist(@PathVariable long id) {
        return service.checklist(id);
    }

    @PutMapping("/{assessmentId}/checklist/{itemId}")
    @PreAuthorize(WRITERS)
    public void record(@PathVariable long assessmentId,
                       @PathVariable long itemId,
                       @Valid @RequestBody RecordResultRequest request) {
        service.record(assessmentId, itemId, request.result(), currentUser.id());
    }

    @GetMapping("/gaps/open")
    public List<GapView> openGaps() {
        return service.openGaps();
    }
}
