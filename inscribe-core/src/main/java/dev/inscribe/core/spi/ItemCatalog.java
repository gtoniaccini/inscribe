package dev.inscribe.core.spi;

import java.util.List;

/**
 * SPI interface for exposing domain-specific catalog items to the AI resolver.
 * Each plugin provides exactly one ItemCatalog bean.
 */
public interface ItemCatalog {

    /**
     * The workflow name this catalog belongs to.
     */
    String getWorkflowName();

    /**
     * Return all available catalog items for AI resolution.
     */
    List<CatalogItem> findAll();

    record CatalogItem(String id, String name, String description) {}
}
