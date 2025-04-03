package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.ToolDependency;

import java.util.List;

/**
 * Repository for managing ToolDependency entities.
 */
@Repository
public interface ToolDependencyRepository extends JpaRepository<ToolDependency, String> {

    /**
     * Find all dependencies for a tool.
     *
     * @param toolId The ID of the tool
     * @return List of dependencies for the tool
     */
    List<ToolDependency> findByToolId(String toolId);

    /**
     * Find all dependencies that depend on a tool.
     *
     * @param dependencyToolId The ID of the dependency tool
     * @return List of dependencies that depend on the tool
     */
    List<ToolDependency> findByDependencyToolId(String dependencyToolId);

    /**
     * Find a dependency by tool ID and dependency tool ID.
     *
     * @param toolId The ID of the tool
     * @param dependencyToolId The ID of the dependency tool
     * @return The dependency if found
     */
    ToolDependency findByToolIdAndDependencyToolId(String toolId, String dependencyToolId);

    /**
     * Count the number of tools that depend on a tool.
     *
     * @param dependencyToolId The ID of the dependency tool
     * @return The number of dependents
     */
    long countByDependencyToolId(String dependencyToolId);
}