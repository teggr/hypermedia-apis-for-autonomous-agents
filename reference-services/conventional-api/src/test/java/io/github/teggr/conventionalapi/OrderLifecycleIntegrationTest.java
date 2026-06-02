package io.github.teggr.conventionalapi;

import io.github.teggr.conventionalapi.dto.CreateOrderRequest;
import io.github.teggr.conventionalapi.dto.OrderResponse;
import io.github.teggr.conventionalapi.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderLifecycleIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void fullLifecycle_pendingToDelivered() {
        // Create
        ResponseEntity<OrderResponse> created = rest.postForEntity(
                "/orders",
                new CreateOrderRequest("customer-1", "Widget order"),
                OrderResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = created.getBody().id();
        assertThat(created.getBody().status()).isEqualTo(OrderStatus.PENDING);

        // Confirm
        OrderResponse confirmed = rest.postForObject("/orders/" + id + "/confirm", null, OrderResponse.class);
        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);

        // Ship
        OrderResponse shipped = rest.postForObject("/orders/" + id + "/ship", null, OrderResponse.class);
        assertThat(shipped.status()).isEqualTo(OrderStatus.SHIPPED);

        // Deliver
        OrderResponse delivered = rest.postForObject("/orders/" + id + "/deliver", null, OrderResponse.class);
        assertThat(delivered.status()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void cancel_fromPendingState() {
        ResponseEntity<OrderResponse> created = rest.postForEntity(
                "/orders",
                new CreateOrderRequest("customer-2", "Gadget order"),
                OrderResponse.class);
        Long id = created.getBody().id();

        OrderResponse cancelled = rest.postForObject("/orders/" + id + "/cancel", null, OrderResponse.class);
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
    }
}
