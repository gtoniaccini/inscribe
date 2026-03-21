package dev.inscribe.ecommerce.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * The ecommerce domain entity — a shopping cart / order.
 * Its id is used as containerId by the core orchestration engine.
 */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String customerName;

    private String customerEmail;

    private String shippingAddress;

    private String paymentMethod;

    private String currency;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Cart() {}

    public Cart(String customerName) {
        this.customerName = customerName;
    }

    public UUID getId() { return id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getCreatedAt() { return createdAt; }
}
