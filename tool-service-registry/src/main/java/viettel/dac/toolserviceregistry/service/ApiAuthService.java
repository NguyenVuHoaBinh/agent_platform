package viettel.dac.toolserviceregistry.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.exception.ResourceNotFoundException;
import viettel.dac.toolserviceregistry.exception.ToolNotFoundException;
import viettel.dac.toolserviceregistry.exception.ToolTypeNotCompatibleException;
import viettel.dac.toolserviceregistry.model.dto.auth.*;
import viettel.dac.toolserviceregistry.model.entity.ApiToolMetadata;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.auth.*;
import viettel.dac.toolserviceregistry.model.enums.AuthenticationType;
import viettel.dac.toolserviceregistry.model.enums.ToolType;
import viettel.dac.toolserviceregistry.repository.ApiAuthConfigRepository;
import viettel.dac.toolserviceregistry.repository.ApiToolMetadataRepository;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing API authentication configurations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiAuthService {
    private final ApiAuthConfigRepository authConfigRepository;
    private final ApiToolMetadataRepository apiToolMetadataRepository;
    private final ToolRepository toolRepository;

    /**
     * Gets authentication configurations for an API tool.
     *
     * @param toolId The ID of the API tool
     * @return List of authentication configurations
     */
    public List<ApiAuthConfigDTO> getAuthConfigs(String toolId) {
        log.debug("Getting authentication configurations for tool: {}", toolId);

        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        if (tool.getToolType() != ToolType.API_TOOL) {
            throw new ToolTypeNotCompatibleException(toolId, "API_TOOL");
        }

        List<ApiAuthConfig> configs = authConfigRepository.findByToolId(toolId);

        return configs.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets a specific authentication configuration.
     *
     * @param configId The ID of the authentication configuration
     * @return The authentication configuration
     */
    public ApiAuthConfigDTO getAuthConfig(String configId) {
        log.debug("Getting authentication configuration: {}", configId);

        ApiAuthConfig config = authConfigRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("API authentication configuration", configId));

        return mapToDto(config);
    }

    /**
     * Creates an API key authentication configuration.
     *
     * @param toolId The ID of the API tool
     * @param config The authentication configuration
     * @return The created authentication configuration
     */
    @Transactional
    public ApiAuthConfigDTO createApiKeyAuth(String toolId, ApiKeyAuthConfigDTO config) {
        log.debug("Creating API key authentication for tool: {}", toolId);

        ApiToolMetadata metadata = getApiToolMetadata(toolId);

        // Create new config
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig();
        authConfig.setId(UUID.randomUUID().toString());
        authConfig.setApiToolMetadata(metadata);
        authConfig.setAuthType(AuthenticationType.API_KEY);
        authConfig.setName(config.getName());
        authConfig.setDescription(config.getDescription());
        authConfig.setEnabled(config.isEnabled());
        authConfig.setApiKey(config.getApiKey());
        authConfig.setKeyName(config.getKeyName());
        authConfig.setKeyLocation(config.getKeyLocation());

        // Save config
        ApiAuthConfig savedConfig = authConfigRepository.save(authConfig);

        return mapToDto(savedConfig);
    }

    /**
     * Creates a basic authentication configuration.
     *
     * @param toolId The ID of the API tool
     * @param config The authentication configuration
     * @return The created authentication configuration
     */
    @Transactional
    public ApiAuthConfigDTO createBasicAuth(String toolId, BasicAuthConfigDTO config) {
        log.debug("Creating basic authentication for tool: {}", toolId);

        ApiToolMetadata metadata = getApiToolMetadata(toolId);

        // Create new config
        BasicAuthConfig authConfig = new BasicAuthConfig();
        authConfig.setId(UUID.randomUUID().toString());
        authConfig.setApiToolMetadata(metadata);
        authConfig.setAuthType(AuthenticationType.BASIC);
        authConfig.setName(config.getName());
        authConfig.setDescription(config.getDescription());
        authConfig.setEnabled(config.isEnabled());
        authConfig.setUsername(config.getUsername());
        authConfig.setPassword(config.getPassword());

        // Save config
        ApiAuthConfig savedConfig = authConfigRepository.save(authConfig);

        return mapToDto(savedConfig);
    }

    /**
     * Creates a bearer token authentication configuration.
     *
     * @param toolId The ID of the API tool
     * @param config The authentication configuration
     * @return The created authentication configuration
     */
    @Transactional
    public ApiAuthConfigDTO createBearerTokenAuth(String toolId, BearerTokenAuthConfigDTO config) {
        log.debug("Creating bearer token authentication for tool: {}", toolId);

        ApiToolMetadata metadata = getApiToolMetadata(toolId);

        // Create new config
        BearerTokenAuthConfig authConfig = new BearerTokenAuthConfig();
        authConfig.setId(UUID.randomUUID().toString());
        authConfig.setApiToolMetadata(metadata);
        authConfig.setAuthType(AuthenticationType.BEARER_TOKEN);
        authConfig.setName(config.getName());
        authConfig.setDescription(config.getDescription());
        authConfig.setEnabled(config.isEnabled());
        authConfig.setToken(config.getToken());
        authConfig.setTokenPrefix(config.getTokenPrefix());

        // Save config
        ApiAuthConfig savedConfig = authConfigRepository.save(authConfig);

        return mapToDto(savedConfig);
    }

    /**
     * Creates an OAuth2 authentication configuration.
     *
     * @param toolId The ID of the API tool
     * @param config The authentication configuration
     * @return The created authentication configuration
     */
    @Transactional
    public ApiAuthConfigDTO createOAuth2Auth(String toolId, OAuth2AuthConfigDTO config) {
        log.debug("Creating OAuth2 authentication for tool: {}", toolId);

        ApiToolMetadata metadata = getApiToolMetadata(toolId);

        // Create new config
        OAuth2AuthConfig authConfig = new OAuth2AuthConfig();
        authConfig.setId(UUID.randomUUID().toString());
        authConfig.setApiToolMetadata(metadata);
        authConfig.setAuthType(AuthenticationType.OAUTH2);
        authConfig.setName(config.getName());
        authConfig.setDescription(config.getDescription());
        authConfig.setEnabled(config.isEnabled());
        authConfig.setClientId(config.getClientId());
        authConfig.setClientSecret(config.getClientSecret());
        authConfig.setTokenUrl(config.getTokenUrl());
        authConfig.setAuthorizationUrl(config.getAuthorizationUrl());
        authConfig.setScope(config.getScope());
        authConfig.setGrantType(config.getGrantType());
        authConfig.setAccessToken(config.getAccessToken());
        authConfig.setRefreshToken(config.getRefreshToken());
        authConfig.setTokenExpiry(config.getTokenExpiry());

        // Save config
        ApiAuthConfig savedConfig = authConfigRepository.save(authConfig);

        return mapToDto(savedConfig);
    }

    /**
     * Updates an authentication configuration.
     *
     * @param configId The ID of the authentication configuration
     * @param config The updated authentication configuration
     * @return The updated authentication configuration
     */
    @Transactional
    public ApiAuthConfigDTO updateAuthConfig(String configId, ApiAuthConfigDTO config) {
        log.debug("Updating authentication configuration: {}", configId);

        ApiAuthConfig authConfig = authConfigRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("API authentication configuration", configId));

        // Update common fields
        authConfig.setName(config.getName());
        authConfig.setDescription(config.getDescription());
        authConfig.setEnabled(config.isEnabled());

        // Update type-specific fields
        if (authConfig instanceof ApiKeyAuthConfig && config instanceof ApiKeyAuthConfigDTO) {
            updateApiKeyAuth((ApiKeyAuthConfig) authConfig, (ApiKeyAuthConfigDTO) config);
        } else if (authConfig instanceof BasicAuthConfig && config instanceof BasicAuthConfigDTO) {
            updateBasicAuth((BasicAuthConfig) authConfig, (BasicAuthConfigDTO) config);
        } else if (authConfig instanceof BearerTokenAuthConfig && config instanceof BearerTokenAuthConfigDTO) {
            updateBearerTokenAuth((BearerTokenAuthConfig) authConfig, (BearerTokenAuthConfigDTO) config);
        } else if (authConfig instanceof OAuth2AuthConfig && config instanceof OAuth2AuthConfigDTO) {
            updateOAuth2Auth((OAuth2AuthConfig) authConfig, (OAuth2AuthConfigDTO) config);
        } else {
            throw new IllegalArgumentException("Authentication configuration type mismatch");
        }

        // Save updated config
        ApiAuthConfig savedConfig = authConfigRepository.save(authConfig);

        return mapToDto(savedConfig);
    }

    /**
     * Deletes an authentication configuration.
     *
     * @param configId The ID of the authentication configuration
     */
    @Transactional
    public void deleteAuthConfig(String configId) {
        log.debug("Deleting authentication configuration: {}", configId);

        ApiAuthConfig authConfig = authConfigRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("API authentication configuration", configId));

        authConfigRepository.delete(authConfig);
    }

    /**
     * Gets the API tool metadata for a tool.
     *
     * @param toolId The ID of the tool
     * @return The API tool metadata
     */
    private ApiToolMetadata getApiToolMetadata(String toolId) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(toolId));

        if (tool.getToolType() != ToolType.API_TOOL) {
            throw new ToolTypeNotCompatibleException(toolId, "API_TOOL");
        }

        return apiToolMetadataRepository.findByToolId(toolId)
                .orElseThrow(() -> new ResourceNotFoundException("API tool metadata", toolId));
    }

    /**
     * Updates an API key authentication configuration.
     *
     * @param authConfig The authentication configuration to update
     * @param config The updated configuration
     */
    private void updateApiKeyAuth(ApiKeyAuthConfig authConfig, ApiKeyAuthConfigDTO config) {
        authConfig.setApiKey(config.getApiKey());
        authConfig.setKeyName(config.getKeyName());
        authConfig.setKeyLocation(config.getKeyLocation());
    }

    /**
     * Updates a basic authentication configuration.
     *
     * @param authConfig The authentication configuration to update
     * @param config The updated configuration
     */
    private void updateBasicAuth(BasicAuthConfig authConfig, BasicAuthConfigDTO config) {
        authConfig.setUsername(config.getUsername());
        authConfig.setPassword(config.getPassword());
    }

    /**
     * Updates a bearer token authentication configuration.
     *
     * @param authConfig The authentication configuration to update
     * @param config The updated configuration
     */
    private void updateBearerTokenAuth(BearerTokenAuthConfig authConfig, BearerTokenAuthConfigDTO config) {
        authConfig.setToken(config.getToken());
        authConfig.setTokenPrefix(config.getTokenPrefix());
    }

    /**
     * Updates an OAuth2 authentication configuration.
     *
     * @param authConfig The authentication configuration to update
     * @param config The updated configuration
     */
    private void updateOAuth2Auth(OAuth2AuthConfig authConfig, OAuth2AuthConfigDTO config) {
        authConfig.setClientId(config.getClientId());
        authConfig.setClientSecret(config.getClientSecret());
        authConfig.setTokenUrl(config.getTokenUrl());
        authConfig.setAuthorizationUrl(config.getAuthorizationUrl());
        authConfig.setScope(config.getScope());
        authConfig.setGrantType(config.getGrantType());
        authConfig.setAccessToken(config.getAccessToken());
        authConfig.setRefreshToken(config.getRefreshToken());
        authConfig.setTokenExpiry(config.getTokenExpiry());
    }

    /**
     * Maps an ApiAuthConfig entity to an ApiAuthConfigDTO.
     *
     * @param config The entity to map
     * @return The mapped DTO
     */
    private ApiAuthConfigDTO mapToDto(ApiAuthConfig config) {
        if (config == null) {
            return null;
        }

        if (config instanceof ApiKeyAuthConfig) {
            ApiKeyAuthConfig apiKeyConfig = (ApiKeyAuthConfig) config;
            return ApiKeyAuthConfigDTO.builder()
                    .id(apiKeyConfig.getId())
                    .authType(apiKeyConfig.getAuthType())
                    .name(apiKeyConfig.getName())
                    .description(apiKeyConfig.getDescription())
                    .enabled(apiKeyConfig.isEnabled())
                    .apiKey(apiKeyConfig.getApiKey())
                    .keyName(apiKeyConfig.getKeyName())
                    .keyLocation(apiKeyConfig.getKeyLocation())
                    .build();
        } else if (config instanceof BasicAuthConfig) {
            BasicAuthConfig basicConfig = (BasicAuthConfig) config;
            return BasicAuthConfigDTO.builder()
                    .id(basicConfig.getId())
                    .authType(basicConfig.getAuthType())
                    .name(basicConfig.getName())
                    .description(basicConfig.getDescription())
                    .enabled(basicConfig.isEnabled())
                    .username(basicConfig.getUsername())
                    .password(basicConfig.getPassword())
                    .build();
        } else if (config instanceof BearerTokenAuthConfig) {
            BearerTokenAuthConfig tokenConfig = (BearerTokenAuthConfig) config;
            return BearerTokenAuthConfigDTO.builder()
                    .id(tokenConfig.getId())
                    .authType(tokenConfig.getAuthType())
                    .name(tokenConfig.getName())
                    .description(tokenConfig.getDescription())
                    .enabled(tokenConfig.isEnabled())
                    .token(tokenConfig.getToken())
                    .tokenPrefix(tokenConfig.getTokenPrefix())
                    .build();
        } else if (config instanceof OAuth2AuthConfig) {
            OAuth2AuthConfig oauth2Config = (OAuth2AuthConfig) config;
            return OAuth2AuthConfigDTO.builder()
                    .id(oauth2Config.getId())
                    .authType(oauth2Config.getAuthType())
                    .name(oauth2Config.getName())
                    .description(oauth2Config.getDescription())
                    .enabled(oauth2Config.isEnabled())
                    .clientId(oauth2Config.getClientId())
                    .clientSecret(oauth2Config.getClientSecret())
                    .tokenUrl(oauth2Config.getTokenUrl())
                    .authorizationUrl(oauth2Config.getAuthorizationUrl())
                    .scope(oauth2Config.getScope())
                    .grantType(oauth2Config.getGrantType())
                    .accessToken(oauth2Config.getAccessToken())
                    .refreshToken(oauth2Config.getRefreshToken())
                    .tokenExpiry(oauth2Config.getTokenExpiry())
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported authentication configuration type");
        }
    }
}