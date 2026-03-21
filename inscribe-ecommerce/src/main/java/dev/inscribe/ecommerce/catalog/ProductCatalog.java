package dev.inscribe.ecommerce.catalog;

import dev.inscribe.core.spi.ItemCatalog;
import dev.inscribe.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductCatalog implements ItemCatalog {

    private final ProductRepository repository;

    public ProductCatalog(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getWorkflowName() {
        return "ecommerce";
    }

    @Override
    public List<CatalogItem> findAll() {
        return repository.findAll().stream()
                .map(p -> new CatalogItem(
                        p.getSku(),
                        p.getName(),
                        "%s — €%s (disponibili: %d)".formatted(
                                p.getDescription(), p.getPrice(), p.getStockQuantity())))
                .toList();
    }
}
