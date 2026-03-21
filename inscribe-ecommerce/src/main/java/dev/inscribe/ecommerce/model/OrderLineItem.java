package dev.inscribe.ecommerce.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An order line-item within a cart.
 * This is the plugin's own table — the core never touches it.
 */
@Entity
@Table(name = "order_line_items")
public class OrderLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID cartId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    private BigDecimal unitPrice;

    @Column(nullable = false, updatable = false)
    private Instant insertedAt = Instant.now();

    protected OrderLineItem() {}

    public OrderLineItem(UUID cartId, String sku, String productName, int quantity) {
        this.cartId = cartId;
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
    }

    public UUID getId() { return id; }
    public UUID getCartId() { return cartId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Instant getInsertedAt() { return insertedAt; }
}
