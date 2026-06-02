package io.github.teggr.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised configuration for the target API.
 *
 * <p>Set {@code agent.api.mode} to {@code "conventional"} or {@code "hypermedia"}
 * to switch which reference service the agent talks to.
 */
@ConfigurationProperties(prefix = "agent.api")
public record AgentApiProperties(
        String mode,
        BaseUrls baseUrl
) {
    public record BaseUrls(String conventional, String hypermedia) {}

    /**
     * Returns the base URL of whichever service is currently selected.
     */
    public String resolvedBaseUrl() {
        return "hypermedia".equalsIgnoreCase(mode)
                ? baseUrl.hypermedia()
                : baseUrl.conventional();
    }
}
