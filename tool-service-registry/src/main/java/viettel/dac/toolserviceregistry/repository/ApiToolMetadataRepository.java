package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.ApiToolMetadata;

import java.util.Optional;

/**
 * Repository for managing ApiToolMetadata entities.
 */
@Repository
public interface ApiToolMetadataRepository extends JpaRepository<ApiToolMetadata, String> {

    /**
     * Find API metadata for a tool.
     *
     * @param toolId The ID of the tool
     * @return An Optional containing the API metadata if found
     */
    Optional<ApiToolMetadata> findByToolId(String toolId);
}