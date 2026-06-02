package io.github.teggr.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Tools for the <em>hypermedia</em> API strategy.
 *
 * <p>Rather than exposing every possible operation as a separate tool, this strategy
 * gives the agent two primitives:
 * <ol>
 *   <li><strong>Navigate</strong> — follow a URL the agent discovered in a previous
 *       response's {@code _links} section.</li>
 *   <li><strong>Create order</strong> — bootstrap the conversation by creating the
 *       first resource (necessary because the agent has no prior URL to follow).</li>
 * </ol>
 *
 * <p>The agent is instructed to inspect the {@code _links} object in every response
 * and select the appropriate link relation name to invoke the next action. It never
 * needs to know or guess endpoint paths — the server communicates them.
 *
 * <p>This approach is expected to:
 * <ul>
 *   <li>Reduce hallucinated / invalid tool calls (the agent cannot attempt an action
 *       the server hasn't advertised for the current state)</li>
 *   <li>Require fewer tool definitions (two vs. seven in the conventional strategy)</li>
 *   <li>Be more resilient to API changes (paths can change without updating tools)</li>
 * </ul>
 */
@Component
public class HypermediaOrderTools {

    private final RestClient client;

    public HypermediaOrderTools(RestClient client) {
        this.client = client;
    }

    @Tool(description = """
            Create a new order and return the order resource with its available next actions
            in the _links section. Always inspect _links after creation to discover what
            you can do next.
            """)
    public JsonNode createOrder(
            @ToolParam(description = "Customer identifier") String customerId,
            @ToolParam(description = "Description of the order") String description) {
        return client.post()
                .uri("/orders")
                .body(new CreateOrderRequest(customerId, description))
                .retrieve()
                .body(JsonNode.class);
    }

    @Tool(description = """
            Navigate to a URL from a _links entry in a previous response.
            Use HTTP GET for reading resources (self, orders links) and
            HTTP POST for triggering actions (confirm, ship, deliver, cancel links).
            Always inspect _links in the response to discover what you can do next.
            Pass the full href value exactly as returned by the server.
            """)
    public JsonNode navigate(
            @ToolParam(description = "The href value from a _links entry") String href,
            @ToolParam(description = "HTTP method: GET or POST") String method) {
        if ("POST".equalsIgnoreCase(method)) {
            return client.post().uri(href).retrieve().body(JsonNode.class);
        }
        return client.get().uri(href).retrieve().body(JsonNode.class);
    }

    private record CreateOrderRequest(String customerId, String description) {}
}
