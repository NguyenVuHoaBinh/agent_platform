package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.ToolExample;

import java.util.List;

/**
 * Repository for managing ToolExample entities.
 */
@Repository
public interface ToolExampleRepository extends JpaRepository<ToolExample, String> {

    /**
     * Find all examples for a tool.
     *
     * @param toolId The ID of the tool
     * @return List of examples for the tool
     */
    List<ToolExample> findByToolId(String toolId);
}