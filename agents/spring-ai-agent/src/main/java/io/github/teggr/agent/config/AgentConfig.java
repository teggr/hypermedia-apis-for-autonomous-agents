package io.github.teggr.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AgentApiProperties.class)
public class AgentConfig {

    @Bean
    RestClient orderApiClient(AgentApiProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.resolvedBaseUrl())
                .defaultHeader("Accept", "application/hal+json, application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
