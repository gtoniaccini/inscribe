package dev.inscribe.api.controller;

import dev.inscribe.core.workflow.WorkflowDefinition;
import dev.inscribe.core.workflow.WorkflowDefinitionLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowDefinitionLoader loader;

    public WorkflowController(WorkflowDefinitionLoader loader) {
        this.loader = loader;
    }

    @GetMapping
    public Collection<WorkflowDefinition> list() {
        return loader.getAll();
    }
}
