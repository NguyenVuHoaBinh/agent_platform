package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.AuthenticationType;
import viettel.dac.toolserviceregistry.model.enums.HttpMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Request model for creating or updating API tool metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiToolMetadataRequest {
    private String id;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    @NotBlank(message = "Endpoint path is required")
    private String endpointPath;

    @NotNull(message = "HTTP method is required")
    private HttpMethod httpMethod;

    private String contentType;

    private AuthenticationType authenticationType = AuthenticationType.NONE;

    private Integer requestTimeoutMs;

    private String responseFormat;

    @Valid
    @Builder.Default
    private List<ApiHeaderRequest> headers = new ArrayList<>();

    private Integer rateLimitRequests;

    private Integer rateLimitPeriodSeconds;

    private Integer retryCount;

    private Integer retryDelayMs;
}