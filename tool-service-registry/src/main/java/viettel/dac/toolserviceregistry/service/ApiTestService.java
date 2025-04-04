package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import viettel.dac.toolserviceregistry.exception.ApiCallException;
import viettel.dac.toolserviceregistry.exception.ApiMetadataNotFoundException;
import viettel.dac.toolserviceregistry.model.dto.ApiToolMetadataDTO;
import viettel.dac.toolserviceregistry.model.dto.auth.*;
import viettel.dac.toolserviceregistry.model.entity.ApiParameterMapping;
import viettel.dac.toolserviceregistry.model.enums.ApiParameterLocation;
import viettel.dac.toolserviceregistry.model.enums.AuthenticationType;
import viettel.dac.toolserviceregistry.repository.ApiParameterMappingRepository;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiTestService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiToolService apiToolService;
    private final ApiAuthService apiAuthService;
    private final ApiParameterMappingRepository apiParameterMappingRepository;
    private final MeterRegistry meterRegistry;

    /**
     * Tests an API call using the tool's metadata and provided parameters.
     *
     * @param toolId The ID of the API tool
     * @param parameters Parameters to use for the API call
     * @return The API response with formatted details
     */
    public Map<String, Object> testApiCall(String toolId, Map<String, Object> parameters) {
        log.debug("Testing API call for tool: {}", toolId);
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Get API metadata
            ApiToolMetadataDTO metadata = apiToolService.getApiMetadata(toolId);
            if (metadata == null) {
                throw new ApiMetadataNotFoundException(toolId);
            }

            // Get parameter mappings
            List<ApiParameterMapping> mappings = apiParameterMappingRepository.findByApiToolMetadataId(metadata.getId());

            // Prepare request
            HttpMethod httpMethod = HttpMethod.valueOf(metadata.getHttpMethod().name());
            HttpHeaders headers = prepareHeaders(metadata, parameters);

            // Build URL with path variables and query parameters
            URI uri = buildUri(metadata, mappings, parameters);

            // Prepare request body if needed
            Object body = prepareRequestBody(mappings, parameters);

            // Create HTTP entity
            HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);

            // Execute request with retry logic
            ResponseEntity<String> response = executeWithRetry(
                    uri, httpMethod, requestEntity, metadata.getRetryCount());

            // Process response
            Map<String, Object> result = processResponse(response, metadata);

            // Record metrics
            long duration = sample.stop(meterRegistry.timer("api.call.duration",
                    "tool", toolId,
                    "endpoint", metadata.getEndpointPath(),
                    "method", metadata.getHttpMethod().name(),
                    "status", String.valueOf(response.getStatusCode().value())));

            meterRegistry.counter("api.call.count",
                    "tool", toolId,
                    "endpoint", metadata.getEndpointPath(),
                    "method", metadata.getHttpMethod().name(),
                    "status", String.valueOf(response.getStatusCode().value())).increment();

            // Add metrics to result
            result.put("durationMs", duration / 1_000_000);

            return result;
        } catch (Exception e) {
            meterRegistry.counter("api.call.error",
                    "tool", toolId,
                    "errorType", e.getClass().getSimpleName()).increment();

            log.error("Error testing API call for tool: {}", toolId, e);
            throw new ApiCallException("Failed to test API call: " + e.getMessage(), e);
        }
    }

    /**
     * Prepares HTTP headers for the API call.
     */
    private HttpHeaders prepareHeaders(ApiToolMetadataDTO metadata, Map<String, Object> parameters) {
        HttpHeaders headers = new HttpHeaders();

        // Set content type
        if (metadata.getContentType() != null && !metadata.getContentType().isEmpty()) {
            headers.setContentType(MediaType.parseMediaType(metadata.getContentType()));
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        // Add standard headers
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Add custom headers from metadata
        metadata.getHeaders().forEach(header -> {
            if (!header.isSensitive() || header.getValue() != null) {
                headers.add(header.getName(), header.getValue());
            }
        });

        // Add authentication headers
        addAuthenticationHeaders(headers, metadata);

        return headers;
    }

    /**
     * Adds authentication headers based on the authentication type.
     */
    private void addAuthenticationHeaders(HttpHeaders headers, ApiToolMetadataDTO metadata) {
        if (metadata.getAuthenticationType() == null ||
                metadata.getAuthenticationType() == AuthenticationType.NONE) {
            return;
        }

        // Get all auth configs for the tool
        List<ApiAuthConfigDTO> authConfigs = apiAuthService.getAuthConfigs(metadata.getId());
        Optional<ApiAuthConfigDTO> authConfig = authConfigs.stream()
                .filter(config -> config.getAuthType() == metadata.getAuthenticationType() && config.isEnabled())
                .findFirst();

        if (!authConfig.isPresent()) {
            log.warn("Authentication type {} is set but no enabled config found",
                    metadata.getAuthenticationType());
            return;
        }

        switch (metadata.getAuthenticationType()) {
            case API_KEY:
                addApiKeyAuth(headers, (ApiKeyAuthConfigDTO) authConfig.get());
                break;
            case BASIC:
                addBasicAuth(headers, (BasicAuthConfigDTO) authConfig.get());
                break;
            case BEARER_TOKEN:
                addBearerTokenAuth(headers, (BearerTokenAuthConfigDTO) authConfig.get());
                break;
            case OAUTH2:
                addOAuth2Auth(headers, (OAuth2AuthConfigDTO) authConfig.get());
                break;
            default:
                log.warn("Unsupported authentication type: {}", metadata.getAuthenticationType());
        }
    }

    /**
     * Adds API key authentication.
     */
    private void addApiKeyAuth(HttpHeaders headers, ApiKeyAuthConfigDTO config) {
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            log.warn("API key is missing for API key authentication");
            return;
        }

        switch (config.getKeyLocation()) {
            case HEADER:
                headers.add(config.getKeyName(), config.getApiKey());
                break;
            case QUERY:
                // This will be handled in URI building
                break;
            case COOKIE:
                headers.add(HttpHeaders.COOKIE,
                        config.getKeyName() + "=" + config.getApiKey());
                break;
            default:
                log.warn("Unsupported API key location: {}", config.getKeyLocation());
        }
    }

    /**
     * Adds basic authentication.
     */
    private void addBasicAuth(HttpHeaders headers, BasicAuthConfigDTO config) {
        if (config.getUsername() == null || config.getPassword() == null) {
            log.warn("Username or password is missing for basic authentication");
            return;
        }

        String auth = config.getUsername() + ":" + config.getPassword();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        headers.add(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth));
    }

    /**
     * Adds bearer token authentication.
     */
    private void addBearerTokenAuth(HttpHeaders headers, BearerTokenAuthConfigDTO config) {
        if (config.getToken() == null || config.getToken().isEmpty()) {
            log.warn("Token is missing for bearer token authentication");
            return;
        }

        String tokenPrefix = config.getTokenPrefix() != null ?
                config.getTokenPrefix() : "Bearer";
        headers.add(HttpHeaders.AUTHORIZATION, tokenPrefix + " " + config.getToken());
    }

    /**
     * Adds OAuth2 authentication.
     */
    private void addOAuth2Auth(HttpHeaders headers, OAuth2AuthConfigDTO config) {
        if (config.getAccessToken() == null || config.getAccessToken().isEmpty()) {
            log.warn("Access token is missing for OAuth2 authentication");
            return;
        }

        // Check if token is expired
        boolean isExpired = config.getTokenExpiry() != null &&
                config.getTokenExpiry() < System.currentTimeMillis();

        if (isExpired) {
            log.warn("OAuth2 access token is expired");
            // In a real implementation, we would refresh the token here
        }

        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAccessToken());
    }

    /**
     * Builds the URI for the API call.
     */
    private URI buildUri(ApiToolMetadataDTO metadata, List<ApiParameterMapping> mappings,
                         Map<String, Object> parameters) {
        String baseUrl = metadata.getBaseUrl();
        String endpointPath = metadata.getEndpointPath();

        // Replace path parameters in the endpoint path
        for (ApiParameterMapping mapping : mappings) {
            if (mapping.getApiLocation() == ApiParameterLocation.PATH) {
                String paramName = mapping.getToolParameter().getName();
                if (parameters.containsKey(paramName)) {
                    String pathVarName = mapping.getApiParameterName();
                    String pathVarValue = String.valueOf(parameters.get(paramName));
                    endpointPath = endpointPath.replace("{" + pathVarName + "}", pathVarValue);
                }
            }
        }

        // Build URI with query parameters
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + endpointPath);

        for (ApiParameterMapping mapping : mappings) {
            if (mapping.getApiLocation() == ApiParameterLocation.QUERY) {
                String paramName = mapping.getToolParameter().getName();
                if (parameters.containsKey(paramName)) {
                    String queryParamName = mapping.getApiParameterName();
                    Object queryParamValue = parameters.get(paramName);
                    builder.queryParam(queryParamName, queryParamValue);
                }
            }
        }

        return builder.build().toUri();
    }

    /**
     * Prepares the request body for the API call.
     */
    private Object prepareRequestBody(List<ApiParameterMapping> mappings, Map<String, Object> parameters) {
        // Filter mappings for body parameters
        List<ApiParameterMapping> bodyMappings = mappings.stream()
                .filter(mapping -> mapping.getApiLocation() == ApiParameterLocation.BODY)
                .collect(Collectors.toList());

        if (bodyMappings.isEmpty()) {
            return null;
        }

        // Create a map for the request body
        Map<String, Object> bodyMap = new HashMap<>();

        for (ApiParameterMapping mapping : bodyMappings) {
            String paramName = mapping.getToolParameter().getName();
            if (parameters.containsKey(paramName)) {
                String bodyParamName = mapping.getApiParameterName();
                Object bodyParamValue = parameters.get(paramName);
                bodyMap.put(bodyParamName, bodyParamValue);
            }
        }

        return bodyMap;
    }

    /**
     * Executes the API call with retry logic.
     */
    private ResponseEntity<String> executeWithRetry(URI uri, HttpMethod method,
                                                    HttpEntity<?> requestEntity, Integer retryCount) {
        int maxRetries = retryCount != null ? retryCount : 0;
        int currentRetry = 0;
        RestClientException lastException = null;

        while (currentRetry <= maxRetries) {
            try {
                return restTemplate.exchange(uri, method, requestEntity, String.class);
            } catch (RestClientException e) {
                lastException = e;
                log.warn("API call failed (retry {}/{}): {}",
                        currentRetry, maxRetries, e.getMessage());
                currentRetry++;

                if (currentRetry <= maxRetries) {
                    // Exponential backoff
                    long sleepTime = (long) Math.pow(2, currentRetry) * 100;
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // If we're here, all retries failed
        throw new ApiCallException("API call failed after " + maxRetries + " retries", lastException);
    }

    /**
     * Processes the API response.
     */
    private Map<String, Object> processResponse(ResponseEntity<String> response,
                                                ApiToolMetadataDTO metadata) {
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", response.getStatusCode().value());
        result.put("success", response.getStatusCode().is2xxSuccessful());
        result.put("headers", formatHeaders(response.getHeaders()));

        // Process response body
        if (response.getBody() != null) {
            String responseBody = response.getBody();
            result.put("rawResponse", responseBody);

            // Try to parse as JSON
            try {
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                result.put("parsedResponse", jsonNode);
                result.put("responseFormat", "json");
            } catch (JsonProcessingException e) {
                log.debug("Response is not valid JSON: {}", e.getMessage());
                result.put("responseFormat", "text");
            }
        }

        return result;
    }

    /**
     * Formats HTTP headers for display.
     */
    private Map<String, List<String>> formatHeaders(HttpHeaders headers) {
        Map<String, List<String>> formattedHeaders = new HashMap<>();

        headers.forEach((name, values) -> {
            // Don't include sensitive headers like Authorization
            if (!HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                formattedHeaders.put(name, new ArrayList<>(values));
            } else {
                formattedHeaders.put(name, Collections.singletonList("*****"));
            }
        });

        return formattedHeaders;
    }
}