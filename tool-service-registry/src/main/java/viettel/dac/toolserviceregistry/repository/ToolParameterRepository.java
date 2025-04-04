package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.ToolParameter;
import viettel.dac.toolserviceregistry.model.enums.ParameterSource;

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

    /**
     * Find all parameters for a tool with a specific source.
     *
     * @param toolId The ID of the tool
     * @param parameterSource The parameter source
     * @return List of parameters for the tool with the given source
     */
    List<ToolParameter> findByToolIdAndParameterSource(String toolId, ParameterSource parameterSource);
}