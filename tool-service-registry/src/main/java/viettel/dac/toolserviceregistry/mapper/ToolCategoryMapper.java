package viettel.dac.toolserviceregistry.mapper;

import org.mapstruct.Mapper;
import viettel.dac.toolserviceregistry.model.dto.ToolCategoryDTO;
import viettel.dac.toolserviceregistry.model.entity.ToolCategory;

import java.util.List;

/**
 * Mapper for ToolCategory entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface ToolCategoryMapper {

    /**
     * Maps a ToolCategory entity to a ToolCategoryDTO.
     *
     * @param category The ToolCategory entity
     * @return The corresponding ToolCategoryDTO
     */
    ToolCategoryDTO toDto(ToolCategory category);

    /**
     * Maps a list of ToolCategory entities to a list of ToolCategoryDTO objects.
     *
     * @param categories The ToolCategory entities
     * @return The corresponding list of ToolCategoryDTO objects
     */
    List<ToolCategoryDTO> toDtoList(List<ToolCategory> categories);
}