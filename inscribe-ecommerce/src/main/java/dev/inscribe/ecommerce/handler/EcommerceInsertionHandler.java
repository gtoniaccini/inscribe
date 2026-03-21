package dev.inscribe.ecommerce.handler;

import dev.inscribe.core.spi.StepHandler;
import dev.inscribe.core.spi.StepResult;
import dev.inscribe.core.spi.WorkflowContext;
import dev.inscribe.ecommerce.model.OrderLineItem;
import dev.inscribe.ecommerce.repository.OrderLineItemRepository;
import dev.inscribe.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Persists the product into the plugin's own table (order_line_items).
 * Called AFTER stock validation has passed.
 */
@Component
public class EcommerceInsertionHandler implements StepHandler {

    private static final Logger log = LoggerFactory.getLogger(EcommerceInsertionHandler.class);

    private final OrderLineItemRepository lineItemRepository;
    private final ProductRepository productRepository;

    public EcommerceInsertionHandler(
            OrderLineItemRepository lineItemRepository,
            ProductRepository productRepository) {
        this.lineItemRepository = lineItemRepository;
        this.productRepository = productRepository;
    }

    @Override
    public String getName() {
        return "EcommerceInsertionHandler";
    }

    @Override
    public StepResult handle(WorkflowContext context) {
        // Read quantity from metadata (default: 1)
        int quantity = 1;
        Object qty = context.metadata().get("quantity");
        if (qty instanceof Number n) {
            quantity = n.intValue();
        }

        OrderLineItem lineItem = new OrderLineItem(
                context.containerId(),
                context.catalogItemId(),
                context.catalogItemName(),
                quantity);

        // Set unit price from product catalog
        productRepository.findBySku(context.catalogItemId())
                .ifPresent(product -> lineItem.setUnitPrice(product.getPrice()));

        lineItemRepository.save(lineItem);
        log.info("[{}] Product {} (qty={}) inserted into order_line_items for cart {}",
                context.correlationId(), context.catalogItemId(), quantity, context.containerId());

        return StepResult.success();
    }
}
