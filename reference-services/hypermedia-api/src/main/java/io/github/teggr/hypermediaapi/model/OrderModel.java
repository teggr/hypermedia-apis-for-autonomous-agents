package io.github.teggr.hypermediaapi.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.teggr.hypermediaapi.domain.Order;
import io.github.teggr.hypermediaapi.domain.OrderStatus;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

/**
 * HAL representation of an order.
 *
 * <p>In addition to the order data the model carries a {@code _links} section
 * that lists only the transitions that are <em>valid from the current state</em>.
 * An agent can inspect these links to discover what it is permitted to do next
 * without any prior knowledge of the full API surface.
 *
 * <p>Example response for a CONFIRMED order:
 * <pre>
 * {
 *   "id": 1,
 *   "customerId": "customer-1",
 *   "description": "Widget order",
 *   "status": "CONFIRMED",
 *   "createdAt": "...",
 *   "updatedAt": "...",
 *   "_links": {
 *     "self":   { "href": "/orders/1" },
 *     "ship":   { "href": "/orders/1/ship" },
 *     "cancel": { "href": "/orders/1/cancel" },
 *     "orders": { "href": "/orders" }
 *   }
 * }
 * </pre>
 */
@Relation(collectionRelation = "orders", itemRelation = "order")
public class OrderModel extends RepresentationModel<OrderModel> {

    private final Long id;
    private final String customerId;
    private final String description;
    private final OrderStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public OrderModel(Order order) {
        this.id          = order.getId();
        this.customerId  = order.getCustomerId();
        this.description = order.getDescription();
        this.status      = order.getStatus();
        this.createdAt   = order.getCreatedAt();
        this.updatedAt   = order.getUpdatedAt();
    }

    public Long getId()            { return id; }
    public String getCustomerId()  { return customerId; }
    public String getDescription() { return description; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }
}
