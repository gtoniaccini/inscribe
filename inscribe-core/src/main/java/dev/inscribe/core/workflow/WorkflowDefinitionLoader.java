package dev.inscribe.core.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers and loads all {@code workflow/*.yml} files from the classpath.
 * Each plugin places its YAML in {@code src/main/resources/workflow/}.
 */
@Component
public class WorkflowDefinitionLoader {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDefinitionLoader.class);
    private final Map<String, WorkflowDefinition> definitions = new ConcurrentHashMap<>();

    @PostConstruct
    void loadAll() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:workflow/*.yml");

        for (Resource resource : resources) {
            try (InputStream is = resource.getInputStream()) {
                Yaml yaml = new Yaml();
                Map<String, Object> root = yaml.load(is);
                @SuppressWarnings("unchecked")
                Map<String, Object> wf = (Map<String, Object>) root.get("workflow");

                String name = (String) wf.get("name");
                String containerLabel = (String) wf.get("container-label");
                String itemLabel = (String) wf.get("item-label");
                String aiPrompt = (String) wf.get("ai-prompt");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawSteps = (List<Map<String, Object>>) wf.get("steps");
                List<WorkflowDefinition.StepDef> steps = rawSteps.stream()
                        .map(s -> new WorkflowDefinition.StepDef(
                                (String) s.get("name"),
                                (String) s.get("handler"),
                                WorkflowDefinition.FailurePolicy.valueOf(
                                        ((String) s.get("on-failure")).toUpperCase())
                        ))
                        .toList();

                @SuppressWarnings("unchecked")
                WorkflowDefinition.TopicDef onComplete = parseTopicDef((Map<String, Object>) wf.get("on-complete"));
                @SuppressWarnings("unchecked")
                WorkflowDefinition.TopicDef onReject = parseTopicDef((Map<String, Object>) wf.get("on-reject"));

                WorkflowDefinition def = new WorkflowDefinition(name, containerLabel, itemLabel, aiPrompt, steps, onComplete, onReject);
                definitions.put(name, def);
                log.info("Loaded workflow definition: {} ({} steps)", name, steps.size());
            }
        }

        if (definitions.isEmpty()) {
            log.warn("No workflow definitions found on classpath (workflow/*.yml)");
        }
    }

    public WorkflowDefinition get(String workflowName) {
        WorkflowDefinition def = definitions.get(workflowName);
        if (def == null) {
            throw new IllegalArgumentException("Unknown workflow: " + workflowName);
        }
        return def;
    }

    public Collection<WorkflowDefinition> getAll() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    private static WorkflowDefinition.TopicDef parseTopicDef(Map<String, Object> map) {
        if (map == null) return null;
        return new WorkflowDefinition.TopicDef((String) map.get("topic"));
    }
}
