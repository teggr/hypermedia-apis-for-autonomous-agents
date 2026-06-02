package io.github.teggr.hypermediaapi.domain;

/**
 * The set of states an order can be in, together with the valid transitions
 * out of each state.
 *
 * <pre>
 *   PENDING ──► CONFIRMED ──► SHIPPED ──► DELIVERED
 *      │             │
 *      └─────────────┴──► CANCELLED
 * </pre>
 */
public enum OrderStatus {

    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /**
     * Returns {@code true} when a transition from this status to {@code target} is permitted.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING    -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED  -> target == SHIPPED   || target == CANCELLED;
            case SHIPPED    -> target == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
