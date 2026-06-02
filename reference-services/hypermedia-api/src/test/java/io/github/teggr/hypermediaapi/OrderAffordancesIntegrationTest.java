package io.github.teggr.hypermediaapi;

import io.github.teggr.hypermediaapi.controller.OrderController;
import io.github.teggr.hypermediaapi.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderAffordancesIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @SuppressWarnings("unchecked")
    void pendingOrder_shouldExposeConfirmAndCancelLinks() {
        Long id = createOrder("customer-1", "Widget order");

        ResponseEntity<Map> response = getOrder(id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> links = (Map<String, Object>) response.getBody().get("_links");
        assertThat(links).containsKey("self");
        assertThat(links).containsKey("confirm");
        assertThat(links).containsKey("cancel");
        // PENDING order must NOT expose ship or deliver
        assertThat(links).doesNotContainKey("ship");
        assertThat(links).doesNotContainKey("deliver");
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmedOrder_shouldExposeShipAndCancelLinks_notConfirm() {
        Long id = createOrder("customer-2", "Gadget order");
        postTransition(id, "confirm");

        Map<String, Object> links = (Map<String, Object>) getOrder(id).getBody().get("_links");
        assertThat(links).containsKey("ship");
        assertThat(links).containsKey("cancel");
        assertThat(links).doesNotContainKey("confirm");
        assertThat(links).doesNotContainKey("deliver");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deliveredOrder_shouldExposeNoTransitionLinks() {
        Long id = createOrder("customer-3", "Doohickey order");
        postTransition(id, "confirm");
        postTransition(id, "ship");
        postTransition(id, "deliver");

        Map<String, Object> links = (Map<String, Object>) getOrder(id).getBody().get("_links");
        assertThat(links).containsKey("self");
        assertThat(links).doesNotContainKey("confirm");
        assertThat(links).doesNotContainKey("ship");
        assertThat(links).doesNotContainKey("deliver");
        assertThat(links).doesNotContainKey("cancel");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Long createOrder(String customerId, String description) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = rest.postForEntity(
                "/orders",
                new HttpEntity<>(Map.of("customerId", customerId, "description", description), headers),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) response.getBody().get("id")).longValue();
    }

    private ResponseEntity<Map> getOrder(Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaTypes.HAL_JSON_VALUE);
        return rest.getForEntity("/orders/" + id, Map.class);
    }

    private void postTransition(Long id, String rel) {
        rest.postForObject("/orders/" + id + "/" + rel, null, Map.class);
    }
}
