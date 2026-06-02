package io.github.teggr.agent.service;

import io.github.teggr.agent.config.AgentApiProperties;
import io.github.teggr.agent.metrics.AgentMetricsCollector;
import io.github.teggr.agent.tools.ConventionalOrderTools;
import io.github.teggr.agent.tools.HypermediaOrderTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the order management agent.
 *
 * <p>The agent is configured with different tool sets depending on the active mode:
 * <ul>
 *   <li>{@code conventional} — seven pre-declared tools mirroring the OpenAPI spec</li>
 *   <li>{@code hypermedia} — two primitive tools (create + navigate), the agent
 *       discovers further actions from {@code _links} in each response</li>
 * </ul>
 */
@Service
public class OrderAgentService {

    private final ChatClient chatClient;
    private final AgentApiProperties properties;
    private final AgentMetricsCollector metricsCollector;
    private final ConventionalOrderTools conventionalTools;
    private final HypermediaOrderTools hypermediaTools;

    public OrderAgentService(
            ChatClient.Builder chatClientBuilder,
            AgentApiProperties properties,
            AgentMetricsCollector metricsCollector,
            ConventionalOrderTools conventionalTools,
            HypermediaOrderTools hypermediaTools) {
        this.chatClient        = chatClientBuilder.build();
        this.properties        = properties;
        this.metricsCollector  = metricsCollector;
        this.conventionalTools = conventionalTools;
        this.hypermediaTools   = hypermediaTools;
    }

    /**
     * Runs the agent against the configured API and returns the final response.
     *
     * @param task natural-language task description, e.g.
     *             "Create an order for customer C1, then confirm and ship it."
     */
    public String run(String task) {
        String systemPrompt = buildSystemPrompt();
        Object tools = resolveTools();
        metricsCollector.startRun(properties.mode(), task);

        try {
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(task)
                    .tools(tools)
                    .call()
                    .content();
            metricsCollector.completeRun(content);
            return content;
        } catch (RuntimeException ex) {
            metricsCollector.failRun(ex.getMessage());
            throw ex;
        }
    }

    private String buildSystemPrompt() {
        if ("hypermedia".equalsIgnoreCase(properties.mode())) {
            return """
                    You are an order management agent working with a hypermedia REST API.
                    
                    Rules:
                    1. After every API call, inspect the _links section of the response.
                    2. Use link relation names (confirm, ship, deliver, cancel) to understand
                       what actions are available from the current state.
                    3. To trigger an action, call the navigate tool with the href from _links
                       and method=POST. To read a resource, use method=GET.
                    4. Never guess or hardcode URLs — always use hrefs from _links.
                    5. Report the final order state when the task is complete.
                    """;
        }
        return """
                You are an order management agent working with a conventional REST API.
                
                Available order statuses and valid transitions:
                - PENDING   → can be CONFIRMED or CANCELLED
                - CONFIRMED → can be SHIPPED or CANCELLED
                - SHIPPED   → can be DELIVERED
                - DELIVERED → terminal state
                - CANCELLED → terminal state
                
                Use the provided tools to create, retrieve, and transition orders.
                Report the final order state when the task is complete.
                """;
    }

    private Object resolveTools() {
        return "hypermedia".equalsIgnoreCase(properties.mode())
                ? hypermediaTools
                : conventionalTools;
    }
}
