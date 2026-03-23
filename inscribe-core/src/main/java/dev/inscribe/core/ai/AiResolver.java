package dev.inscribe.core.ai;

import dev.inscribe.core.spi.ItemCatalog;
import dev.inscribe.core.spi.ItemCatalog.CatalogItem;
import dev.inscribe.core.workflow.WorkflowDefinition;
import dev.inscribe.core.workflow.WorkflowDefinitionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Translates a natural-language user prompt into a list of catalog item IDs
 * using OpenAI function calling.
 */
@Service
@ConditionalOnProperty(name = "inscribe.ai.enabled", havingValue = "true", matchIfMissing = true)
public class AiResolver {

    private static final Logger log = LoggerFactory.getLogger(AiResolver.class);

    private final ChatClient chatClient;
    private final Map<String, ItemCatalog> catalogs;
    private final WorkflowDefinitionLoader workflowLoader;

    public AiResolver(ChatClient.Builder chatClientBuilder,
                      List<ItemCatalog> catalogBeans,
                      WorkflowDefinitionLoader workflowLoader) {
        this.chatClient = chatClientBuilder.build();
        this.catalogs = catalogBeans.stream()
                .collect(Collectors.toMap(ItemCatalog::getWorkflowName, Function.identity()));
        this.workflowLoader = workflowLoader;
    }

    /**
     * Resolve a natural-language prompt into catalog item IDs for a given workflow.
     */
    public List<CatalogItem> resolve(String workflowName, String userPrompt) {
        ItemCatalog catalog = catalogs.get(workflowName);
        if (catalog == null) {
            throw new IllegalArgumentException("No ItemCatalog for workflow: " + workflowName);
        }

        WorkflowDefinition workflow = workflowLoader.get(workflowName);
        List<CatalogItem> allItems = catalog.findAll();

        String catalogDescription = allItems.stream()
                .map(item -> "- ID: %s | Name: %s | Description: %s"
                        .formatted(item.id(), item.name(), item.description()))
                .collect(Collectors.joining("\n"));

        String systemPrompt = """
                You are an assistant that maps natural language requests to catalog items.
                
                Available items:
                %s
                
                Instructions:
                - Given the user's request, return ONLY a JSON array of item IDs that match.
                - Example response: ["ID1", "ID2"]
                - If no items match, return an empty array: []
                - Do not include any explanation, only the JSON array.
                """.formatted(catalogDescription);

        String aiPromptTemplate = workflow.aiPrompt();
        String filledPrompt = aiPromptTemplate.replace("{userInput}", userPrompt);

        log.info("Resolving AI prompt for workflow '{}': {}", workflowName, filledPrompt);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(filledPrompt)
                .call()
                .content();

        log.debug("AI response: {}", response);

        return parseItemIds(response, allItems);
    }

    private List<CatalogItem> parseItemIds(String response, List<CatalogItem> allItems) {
        // Parse the JSON array of IDs from the response
        String cleaned = response.strip()
                .replaceAll("^```json\\s*", "")
                .replaceAll("```$", "")
                .strip();

        Map<String, CatalogItem> itemMap = allItems.stream()
                .collect(Collectors.toMap(CatalogItem::id, Function.identity()));

        // Simple JSON array parsing — ["id1", "id2"]
        return java.util.Arrays.stream(
                        cleaned.replaceAll("[\\[\\]\"]", "").split(","))
                .map(String::strip)
                .filter(id -> !id.isEmpty())
                .map(id -> {
                    CatalogItem item = itemMap.get(id);
                    if (item == null) {
                        log.warn("AI returned unknown item ID: {}", id);
                    }
                    return item;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
