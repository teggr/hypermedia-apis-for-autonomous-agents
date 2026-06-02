package io.github.teggr.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.teggr.agent.metrics.AgentMetricsCollector;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Tools for the <em>conventional</em> API strategy.
 *
 * <p>The agent is given a fixed set of tools whose URLs and parameters are
 * pre-declared here. There is no runtime discovery — the agent must choose
 * the correct tool based on its pre-loaded knowledge of the API contract.
 *
 * <p>This mirrors the typical MCP / OpenAPI-tool approach.
 */
@Component
public class ConventionalOrderTools {

    private final RestClient client;
    private final AgentMetricsCollector metricsCollector;

    public ConventionalOrderTools(RestClient client, AgentMetricsCollector metricsCollector) {
        this.client = client;
        this.metricsCollector = metricsCollector;
    }

    @Tool(description = "Create a new order for a customer")
    public JsonNode createOrder(
            @ToolParam(description = "Customer identifier") String customerId,
            @ToolParam(description = "Description of the order") String description) {
        return executeWithMetrics(() -> client.post()
            .uri("/orders")
            .body(new CreateOrderRequest(customerId, description))
            .retrieve()
            .body(JsonNode.class));
    }

    @Tool(description = "Get a specific order by its ID")
    public JsonNode getOrder(
            @ToolParam(description = "Order ID") long orderId) {
        return executeWithMetrics(() -> client.get()
            .uri("/orders/{id}", orderId)
            .retrieve()
            .body(JsonNode.class));
    }

    @Tool(description = "List all orders, optionally filtered by customer ID or status")
    public JsonNode listOrders(
            @ToolParam(description = "Optional customer ID filter", required = false) String customerId,
            @ToolParam(description = "Optional status filter (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)", required = false) String status) {
        String uri = buildListUri(customerId, status);
        return executeWithMetrics(() -> client.get().uri(uri).retrieve().body(JsonNode.class));
    }

    @Tool(description = "Confirm a pending order — only valid when the order status is PENDING")
    public JsonNode confirmOrder(
            @ToolParam(description = "Order ID") long orderId) {
        return executeWithMetrics(() -> client.post().uri("/orders/{id}/confirm", orderId).retrieve().body(JsonNode.class));
    }

    @Tool(description = "Mark a confirmed order as shipped — only valid when the order status is CONFIRMED")
    public JsonNode shipOrder(
            @ToolParam(description = "Order ID") long orderId) {
        return executeWithMetrics(() -> client.post().uri("/orders/{id}/ship", orderId).retrieve().body(JsonNode.class));
    }

    @Tool(description = "Mark a shipped order as delivered — only valid when the order status is SHIPPED")
    public JsonNode deliverOrder(
            @ToolParam(description = "Order ID") long orderId) {
        return executeWithMetrics(() -> client.post().uri("/orders/{id}/deliver", orderId).retrieve().body(JsonNode.class));
    }

    @Tool(description = "Cancel an order — only valid when the order status is PENDING or CONFIRMED")
    public JsonNode cancelOrder(
            @ToolParam(description = "Order ID") long orderId) {
        return executeWithMetrics(() -> client.post().uri("/orders/{id}/cancel", orderId).retrieve().body(JsonNode.class));
    }

    private JsonNode executeWithMetrics(ApiCall call) {
        metricsCollector.recordApiCall();
        try {
            return call.execute();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                metricsCollector.recordInvalidAttempt();
            }
            throw ex;
        }
    }

    private String buildListUri(String customerId, String status) {
        if (customerId != null) return "/orders?customerId=" + customerId;
        if (status != null) return "/orders?status=" + status;
        return "/orders";
    }

    @FunctionalInterface
    private interface ApiCall {
        JsonNode execute();
    }

    private record CreateOrderRequest(String customerId, String description) {}
}
