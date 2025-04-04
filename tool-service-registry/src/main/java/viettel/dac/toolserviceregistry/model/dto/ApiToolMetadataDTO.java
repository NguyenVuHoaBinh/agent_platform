package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.AuthenticationType;
import viettel.dac.toolserviceregistry.model.enums.HttpMethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for transferring API tool metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiToolMetadataDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String baseUrl;
    private String endpointPath;
    private HttpMethod httpMethod;
    private String contentType;
    private AuthenticationType authenticationType;
    private Integer requestTimeoutMs;
    private String responseFormat;

    @Builder.Default
    private List<ApiHeaderDTO> headers = new ArrayList<>();

    private Integer rateLimitRequests;
    private Integer rateLimitPeriodSeconds;
    private Integer retryCount;
    private Integer retryDelayMs;
}