package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.ApiHeader;

import java.util.List;

/**
 * Repository for managing ApiHeader entities.
 */
@Repository
public interface ApiHeaderRepository extends JpaRepository<ApiHeader, String> {

    /**
     * Find headers for API metadata.
     *
     * @param apiMetadataId The ID of the API metadata
     * @return List of headers
     */
    List<ApiHeader> findByApiToolMetadataId(String apiMetadataId);
}