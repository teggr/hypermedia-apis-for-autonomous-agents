package io.github.teggr.hypermediaapi.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Order() {}

    public Order(String customerId, String description) {
        this.customerId  = customerId;
        this.description = description;
        this.status      = OrderStatus.PENDING;
        this.createdAt   = Instant.now();
        this.updatedAt   = this.createdAt;
    }

    public Long getId()            { return id; }
    public String getCustomerId()  { return customerId; }
    public String getDescription() { return description; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt   = Instant.now();
    }

    /**
     * Attempts to transition this order to {@code newStatus}.
     *
     * @throws IllegalStateException when the transition is not permitted.
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition order %d from %s to %s".formatted(id, status, newStatus));
        }
        this.status    = newStatus;
        this.updatedAt = Instant.now();
    }
}
