package dev.inscribe.ecommerce.controller;

import dev.inscribe.core.ai.AiResolver;
import dev.inscribe.core.command.InsertItemCommand;
import dev.inscribe.core.engine.Orchestrator;
import dev.inscribe.core.spi.ItemCatalog.CatalogItem;
import dev.inscribe.ecommerce.model.Cart;
import dev.inscribe.ecommerce.model.OrderLineItem;
import dev.inscribe.ecommerce.repository.CartRepository;
import dev.inscribe.ecommerce.repository.OrderLineItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private static final String WORKFLOW = "ecommerce";

    private final CartRepository cartRepository;
    private final OrderLineItemRepository lineItemRepository;
    private final Orchestrator orchestrator;
    private final AiResolver aiResolver;

    public CartController(
            CartRepository cartRepository,
            OrderLineItemRepository lineItemRepository,
            Orchestrator orchestrator,
            AiResolver aiResolver) {
        this.cartRepository = cartRepository;
        this.lineItemRepository = lineItemRepository;
        this.orchestrator = orchestrator;
        this.aiResolver = aiResolver;
    }

    // --- CRUD ---

    @PostMapping
    public ResponseEntity<CartResponse> create(@RequestBody CreateCartRequest request) {
        Cart cart = new Cart(request.customerName());
        cart.setCustomerEmail(request.customerEmail());
        cart.setShippingAddress(request.shippingAddress());
        cart.setPaymentMethod(request.paymentMethod());
        cart.setCurrency(request.currency());
        cartRepository.save(cart);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(cart));
    }

    @GetMapping("/{id}")
    public CartDetailResponse get(@PathVariable UUID id) {
        Cart cart = findCart(id);
        List<OrderLineItem> items = lineItemRepository.findByCartIdOrderByInsertedAtAsc(id);
        return toDetailResponse(cart, items);
    }

    @GetMapping
    public List<CartResponse> list() {
        return cartRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // --- Add items ---

    @PostMapping("/{id}/items")
    public ResponseEntity<Void> addItem(
            @PathVariable UUID id,
            @RequestBody AddItemRequest request) {
        findCart(id); // verify exists
        InsertItemCommand command = new InsertItemCommand(
                id, WORKFLOW, request.sku(), request.productName(), request.metadata());
        orchestrator.submit(command);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/items/ai")
    public ResponseEntity<List<String>> addItemsViaAi(
            @PathVariable UUID id,
            @RequestBody AiPromptRequest request) {
        findCart(id); // verify exists
        List<CatalogItem> resolved = aiResolver.resolve(WORKFLOW, request.prompt());

        for (CatalogItem item : resolved) {
            orchestrator.submit(new InsertItemCommand(id, WORKFLOW, item.id(), item.name()));
        }

        return ResponseEntity.accepted()
                .body(resolved.stream().map(CatalogItem::name).toList());
    }

    // --- Helpers ---

    private Cart findCart(UUID id) {
        return cartRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cart not found: " + id));
    }

    private CartResponse toResponse(Cart c) {
        return new CartResponse(c.getId(), c.getCustomerName(),
                c.getCustomerEmail(), c.getCreatedAt());
    }

    private CartDetailResponse toDetailResponse(Cart c, List<OrderLineItem> items) {
        List<LineItemResponse> itemResponses = items.stream()
                .map(i -> new LineItemResponse(i.getSku(), i.getProductName(),
                        i.getQuantity(), i.getUnitPrice(), i.getInsertedAt()))
                .toList();
        return new CartDetailResponse(c.getId(), c.getCustomerName(),
                c.getCustomerEmail(), c.getShippingAddress(), c.getPaymentMethod(),
                c.getCurrency(), c.getCreatedAt(), itemResponses);
    }

    // --- DTOs ---

    public record CreateCartRequest(String customerName, String customerEmail,
                                     String shippingAddress, String paymentMethod,
                                     String currency) {}

    public record AddItemRequest(String sku, String productName, Map<String, Object> metadata) {}

    public record AiPromptRequest(String prompt) {}

    public record CartResponse(UUID id, String customerName, String customerEmail,
                                java.time.Instant createdAt) {}

    public record CartDetailResponse(UUID id, String customerName, String customerEmail,
                                      String shippingAddress, String paymentMethod, String currency,
                                      java.time.Instant createdAt, List<LineItemResponse> items) {}

    public record LineItemResponse(String sku, String productName, int quantity,
                                    BigDecimal unitPrice, java.time.Instant insertedAt) {}
}
