package dev.inscribe.ecommerce.repository;

import dev.inscribe.ecommerce.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
}
