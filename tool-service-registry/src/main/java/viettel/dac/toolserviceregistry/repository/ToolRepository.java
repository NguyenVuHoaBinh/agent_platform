package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolDependency;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Tool entities.
 */
@Repository
public interface ToolRepository extends JpaRepository<Tool, String>, JpaSpecificationExecutor<Tool> {

    /**
     * Find a tool by its name.
     *
     * @param name The name of the tool
     * @return An Optional containing the tool if found
     */
    Optional<Tool> findByName(String name);

    /**
     * Find all active tools.
     *
     * @return List of active tools
     */
    List<Tool> findAllByActiveTrue();

    /**
     * Find a tool with its parameters eagerly loaded.
     *
     * @param id The ID of the tool
     * @return An Optional containing the tool with its parameters if found
     */
    @Query("SELECT t FROM Tool t JOIN FETCH t.parameters WHERE t.id = :id")
    Optional<Tool> findByIdWithParameters(@Param("id") String id);

    /**
     * Find all dependencies across all tools.
     *
     * @return List of all tool dependencies
     */
    @Query("SELECT d FROM ToolDependency d")
    List<ToolDependency> findAllDependencies();

    /**
     * Check if a tool exists with the given name, excluding the given ID.
     *
     * @param name The name to check
     * @param id The ID to exclude
     * @return true if a tool exists with the given name and a different ID
     */
    boolean existsByNameAndIdNot(String name, String id);
}