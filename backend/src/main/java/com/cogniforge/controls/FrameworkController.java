package com.cogniforge.controls;

import com.cogniforge.controls.FrameworkService.FrameworkSummary;
import com.cogniforge.controls.FrameworkService.RequirementView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Any signed-in user may read frameworks. SecurityConfig already requires authentication.
@RestController
@RequestMapping("/api/frameworks")
public class FrameworkController {

    private final FrameworkService service;

    public FrameworkController(FrameworkService service) {
        this.service = service;
    }

    @GetMapping
    public List<FrameworkSummary> list() {
        return service.listFrameworks();
    }

    @GetMapping("/{code}/requirements")
    public List<RequirementView> requirements(@PathVariable String code) {
        return service.listRequirements(code);
    }
}
