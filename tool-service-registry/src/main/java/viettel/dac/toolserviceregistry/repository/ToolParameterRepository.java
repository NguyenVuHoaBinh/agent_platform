package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.ToolParameter;

import java.util.List;

/**
 * Repository for managing ToolParameter entities.
 */
@Repository
public interface ToolParameterRepository extends JpaRepository<ToolParameter, String> {

    /**
     * Find all parameters for a tool.
     *
     * @param toolId The ID of the tool
     * @return List of parameters for the tool
     */
    List<ToolParameter> findByToolId(String toolId);
}