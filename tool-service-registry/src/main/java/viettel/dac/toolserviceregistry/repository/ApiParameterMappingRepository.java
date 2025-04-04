package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.ApiParameterMapping;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ApiParameterMapping entities.
 */
@Repository
public interface ApiParameterMappingRepository extends JpaRepository<ApiParameterMapping, String> {

    /**
     * Find mappings for an API tool metadata.
     *
     * @param apiMetadataId The ID of the API metadata
     * @return List of parameter mappings
     */
    List<ApiParameterMapping> findByApiToolMetadataId(String apiMetadataId);

    /**
     * Find mappings for a tool parameter.
     *
     * @param toolParameterId The ID of the tool parameter
     * @return List of parameter mappings
     */
    List<ApiParameterMapping> findByToolParameterId(String toolParameterId);

    /**
     * Find mapping by API metadata ID and tool parameter ID.
     *
     * @param apiMetadataId The ID of the API metadata
     * @param toolParameterId The ID of the tool parameter
     * @return The parameter mapping if found
     */
    Optional<ApiParameterMapping> findByApiToolMetadataIdAndToolParameterId(
            String apiMetadataId, String toolParameterId);
}