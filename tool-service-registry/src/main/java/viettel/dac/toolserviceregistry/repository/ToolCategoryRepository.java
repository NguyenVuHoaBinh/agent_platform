package viettel.dac.toolserviceregistry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import viettel.dac.toolserviceregistry.model.entity.ToolCategory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for managing ToolCategory entities.
 */
@Repository
public interface ToolCategoryRepository extends JpaRepository<ToolCategory, String> {

    /**
     * Find a category by its name.
     *
     * @param name The name of the category
     * @return An Optional containing the category if found
     */
    Optional<ToolCategory> findByName(String name);

    /**
     * Find all categories with the given IDs.
     *
     * @param ids The IDs of the categories
     * @return Set of categories with the given IDs
     */
    Set<ToolCategory> findByIdIn(List<String> ids);
}