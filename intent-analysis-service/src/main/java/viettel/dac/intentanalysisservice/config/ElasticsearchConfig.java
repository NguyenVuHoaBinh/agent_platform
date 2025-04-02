package viettel.dac.intentanalysisservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.time.Duration;

/**
 * Configuration for Elasticsearch.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "viettel.dac.intentanalysisservice.query.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.rest.uris}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.rest.username:}")
    private String elasticsearchUsername;

    @Value("${spring.elasticsearch.rest.password:}")
    private String elasticsearchPassword;

    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.TerminalClientConfigurationBuilder builder = ClientConfiguration.builder()
                .connectedTo(elasticsearchUri)
                .withConnectTimeout(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.ofSeconds(10));

        // Add credentials if provided
        if (!elasticsearchUsername.isEmpty() && !elasticsearchPassword.isEmpty()) {
            builder = builder.withBasicAuth(elasticsearchUsername, elasticsearchPassword);
        }

        return builder.build();
    }


}