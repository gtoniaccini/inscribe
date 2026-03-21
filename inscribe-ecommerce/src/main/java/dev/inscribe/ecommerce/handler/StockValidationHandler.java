package dev.inscribe.ecommerce.handler;

import dev.inscribe.core.spi.StepHandler;
import dev.inscribe.core.spi.StepResult;
import dev.inscribe.core.spi.WorkflowContext;
import dev.inscribe.ecommerce.model.Product;
import dev.inscribe.ecommerce.repository.OrderLineItemRepository;
import dev.inscribe.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Validates that the product exists, has sufficient stock,
 * and has not already been inserted for this container (idempotency).
 */
@Component
public class StockValidationHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(StockValidationHandler.class);

    private final ProductRepository productRepository;
    private final OrderLineItemRepository orderLineItemRepository;

    public StockValidationHandler(
            ProductRepository productRepository,
            OrderLineItemRepository orderLineItemRepository) {
        this.productRepository = productRepository;
        this.orderLineItemRepository = orderLineItemRepository;
    }

    @Override
    public String getName() {
        return "StockValidationHandler";
    }

    @Override
    public StepResult handle(WorkflowContext context) {
        String sku = context.catalogItemId();
        log.debug("[{}] Validating stock for product: {}", context.correlationId(), sku);

        Optional<Product> product = productRepository.findBySku(sku);
        if (product.isEmpty()) {
            return StepResult.reject("Unknown product SKU: " + sku);
        }
        if (product.get().getStockQuantity() <= 0) {
            return StepResult.reject("Product out of stock: " + sku);
        }

        // Idempotency: check if this SKU was already inserted for this cart
        boolean alreadyInserted = orderLineItemRepository
                .findByCartIdOrderByInsertedAtAsc(context.containerId())
                .stream()
                .anyMatch(item -> item.getSku().equals(sku));
        if (alreadyInserted) {
            return StepResult.reject("Product " + sku + " already present in this cart");
        }

        return StepResult.success();
    }
}
