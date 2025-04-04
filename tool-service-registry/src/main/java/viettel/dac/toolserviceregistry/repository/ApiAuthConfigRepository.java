package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.auth.ApiAuthConfig;
import viettel.dac.toolserviceregistry.model.enums.AuthenticationType;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing API authentication configurations.
 */
@Repository
public interface ApiAuthConfigRepository extends JpaRepository<ApiAuthConfig, String> {

    /**
     * Find all authentication configurations for an API tool metadata.
     *
     * @param apiMetadataId The ID of the API tool metadata
     * @return List of authentication configurations
     */
    List<ApiAuthConfig> findByApiToolMetadataId(String apiMetadataId);

    /**
     * Find authentication configuration for an API tool metadata and auth type.
     *
     * @param apiMetadataId The ID of the API tool metadata
     * @param authType The authentication type
     * @return Optional authentication configuration
     */
    Optional<ApiAuthConfig> findByApiToolMetadataIdAndAuthType(String apiMetadataId, AuthenticationType authType);

    /**
     * Find authentication configuration for a tool ID.
     *
     * @param toolId The ID of the tool
     * @return List of authentication configurations
     */
    @Query("SELECT c FROM ApiAuthConfig c JOIN c.apiToolMetadata m JOIN m.tool t WHERE t.id = :toolId")
    List<ApiAuthConfig> findByToolId(String toolId);
}