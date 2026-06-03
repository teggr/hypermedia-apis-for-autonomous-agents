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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderAffordancesIntegrationTest {

    private static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";

    @Autowired
    private TestRestTemplate rest;

    @Test
    @SuppressWarnings("unchecked")
    void root_shouldExposeOrderNavigationAndCreateTemplate_inHalForms() {
        ResponseEntity<Map> response = getRoot();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString())
                .contains(MediaTypes.HAL_FORMS_JSON_VALUE);

        Map<String, Object> links = (Map<String, Object>) response.getBody().get("_links");
        assertThat(links).containsKey("self");
        assertThat(links).containsKey("orders");
        assertThat(links).containsKey("order");
        Map<String, Object> orderLink = (Map<String, Object>) links.get("order");
        assertThat((String) orderLink.get("href")).endsWith("/orders/{id}");
        assertThat((String) orderLink.get("href")).doesNotContain("%7Bid%7D");
        assertThat(orderLink).containsEntry("templated", true);

        Map<String, Object> templates = (Map<String, Object>) response.getBody().get("_templates");
        assertThat(templates).isNotNull();
        assertThat(templates).containsKey("default");
    }

    @Test
    @SuppressWarnings("unchecked")
    void root_browserAcceptWildcard_shouldStillReturnHalFormsTemplates() {
        ResponseEntity<Map> response = getRootWithAccept(BROWSER_ACCEPT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString())
                .contains(MediaTypes.HAL_FORMS_JSON_VALUE);

        Map<String, Object> templates = (Map<String, Object>) response.getBody().get("_templates");
        assertThat(templates).isNotNull();
        assertThat(templates).containsKey("default");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listOrders_shouldExposeCreateTemplate_inHalForms() {
        ResponseEntity<Map> response = getOrders();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> templates = (Map<String, Object>) response.getBody().get("_templates");
        assertThat(templates).isNotNull();
        assertThat(templates).containsKey("default");
    }

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

        Map<String, Object> templates = (Map<String, Object>) response.getBody().get("_templates");
        assertThat(templates).isNotNull();
        assertThat(templates).containsKey("confirmOrder");
        assertThat(templates).containsKey("cancelOrder");
        assertThat(templates).doesNotContainKey("shipOrder");
        assertThat(templates).doesNotContainKey("deliverOrder");
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
        headers.set("Accept", MediaTypes.HAL_FORMS_JSON_VALUE);
        return rest.exchange("/orders/" + id, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private ResponseEntity<Map> getRoot() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaTypes.HAL_FORMS_JSON_VALUE);
        return rest.exchange("/", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private ResponseEntity<Map> getRootWithAccept(String accept) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", accept);
        return rest.exchange("/", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private ResponseEntity<Map> getOrders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaTypes.HAL_FORMS_JSON_VALUE);
        return rest.exchange("/orders", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private void postTransition(Long id, String rel) {
        rest.postForObject("/orders/" + id + "/" + rel, null, Map.class);
    }
}
