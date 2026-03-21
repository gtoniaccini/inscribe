package dev.inscribe.ecommerce.repository;

import dev.inscribe.ecommerce.model.OrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderLineItemRepository extends JpaRepository<OrderLineItem, UUID> {
    List<OrderLineItem> findByCartIdOrderByInsertedAtAsc(UUID cartId);
}
