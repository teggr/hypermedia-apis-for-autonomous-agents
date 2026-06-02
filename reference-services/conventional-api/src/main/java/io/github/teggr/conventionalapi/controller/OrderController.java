package io.github.teggr.conventionalapi.controller;

import io.github.teggr.conventionalapi.domain.Order;
import io.github.teggr.conventionalapi.domain.OrderStatus;
import io.github.teggr.conventionalapi.dto.CreateOrderRequest;
import io.github.teggr.conventionalapi.dto.OrderResponse;
import io.github.teggr.conventionalapi.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Conventional REST controller for order management.
 *
 * <p>This is the baseline approach: a static set of endpoints documented via OpenAPI.
 * An agent using this API must be pre-loaded with knowledge of every endpoint, parameter,
 * and valid state transition — there is nothing in the response that guides the agent
 * toward what it can do next.
 */
@Tag(name = "Orders", description = "Order lifecycle management")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Operation(summary = "List all orders")
    @GetMapping
    public List<OrderResponse> listOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status) {
        if (customerId != null) {
            return service.findByCustomer(customerId).stream().map(this::toResponse).toList();
        }
        if (status != null) {
            return service.findByStatus(status).stream().map(this::toResponse).toList();
        }
        return service.findAll().stream().map(this::toResponse).toList();
    }

    @Operation(summary = "Create a new order")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order created = service.create(request.customerId(), request.description());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(toResponse(created));
    }

    @Operation(summary = "Get a single order by ID")
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return toResponse(service.findById(id));
    }

    @Operation(summary = "Confirm a pending order")
    @PostMapping("/{id}/confirm")
    public OrderResponse confirmOrder(@PathVariable Long id) {
        return toResponse(service.transition(id, OrderStatus.CONFIRMED));
    }

    @Operation(summary = "Mark a confirmed order as shipped")
    @PostMapping("/{id}/ship")
    public OrderResponse shipOrder(@PathVariable Long id) {
        return toResponse(service.transition(id, OrderStatus.SHIPPED));
    }

    @Operation(summary = "Mark a shipped order as delivered")
    @PostMapping("/{id}/deliver")
    public OrderResponse deliverOrder(@PathVariable Long id) {
        return toResponse(service.transition(id, OrderStatus.DELIVERED));
    }

    @Operation(summary = "Cancel a pending or confirmed order")
    @PostMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long id) {
        return toResponse(service.transition(id, OrderStatus.CANCELLED));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleInvalidTransition() {}

    // ── private helpers ──────────────────────────────────────────────────────

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getDescription(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
