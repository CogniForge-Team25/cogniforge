package com.cogniforge.controls;

import com.cogniforge.controls.ControlMappingService.LinkedRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/controls/{controlId}/requirements")
public class ControlMappingController {

    public record ReplaceMappingRequest(
            @NotNull @Size(max = 200) List<@NotNull Long> requirementIds) {}

    private final ControlMappingService service;

    public ControlMappingController(ControlMappingService service) {
        this.service = service;
    }

    @GetMapping
    public List<LinkedRequirement> list(@PathVariable long controlId) {
        return service.list(controlId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SECURITY_MANAGER','SECURITY_OFFICER')")
    public List<LinkedRequirement> replace(@PathVariable long controlId,
                                           @Valid @RequestBody ReplaceMappingRequest request) {
        return service.replace(controlId, request.requirementIds());
    }
}
