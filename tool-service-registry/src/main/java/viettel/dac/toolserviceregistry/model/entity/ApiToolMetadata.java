package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.AuthenticationType;
import viettel.dac.toolserviceregistry.model.enums.HttpMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing API-specific metadata for API tools.
 */
@Entity
@Table(name = "api_tool_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiToolMetadata {
    @Id
    private String id;

    @OneToOne
    @JoinColumn(name = "tool_id")
    private Tool tool;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "endpoint_path")
    private String endpointPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method")
    private HttpMethod httpMethod;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "authentication_type")
    @Enumerated(EnumType.STRING)
    private AuthenticationType authenticationType;

    @Column(name = "request_timeout_ms")
    private Integer requestTimeoutMs;

    @Column(name = "response_format")
    private String responseFormat;

    @OneToMany(mappedBy = "apiToolMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApiHeader> headers = new ArrayList<>();

    @Column(name = "rate_limit_requests")
    private Integer rateLimitRequests;

    @Column(name = "rate_limit_period_seconds")
    private Integer rateLimitPeriodSeconds;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "retry_delay_ms")
    private Integer retryDelayMs;

    /**
     * Adds a header to the API metadata
     * @param header The header to add
     */
    public void addHeader(ApiHeader header) {
        headers.add(header);
        header.setApiToolMetadata(this);
    }
}