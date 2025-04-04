package viettel.dac.toolserviceregistry.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import viettel.dac.toolserviceregistry.model.dto.ToolDTO;
import viettel.dac.toolserviceregistry.model.entity.Tool;
import viettel.dac.toolserviceregistry.model.entity.ToolCategory;
import viettel.dac.toolserviceregistry.model.reponse.ToolDetailResponse;
import viettel.dac.toolserviceregistry.model.reponse.ToolSummary;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Tool entities and DTOs.
 */
@Mapper(componentModel = "spring", uses = {ToolParameterMapper.class, ToolDependencyMapper.class, ToolCategoryMapper.class, ToolExampleMapper.class})
public interface ToolMapper {

    /**
     * Maps a Tool entity to a ToolDTO.
     *
     * @param tool The Tool entity
     * @return The corresponding ToolDTO
     */
    @Mapping(target = "categories", source = "categories")
    ToolDTO toDto(Tool tool);

    /**
     * Maps a Tool entity to a ToolDetailResponse.
     *
     * @param tool The Tool entity
     * @return The corresponding ToolDetailResponse
     */
    ToolDetailResponse toDetailResponse(Tool tool);

    /**
     * Maps a Tool entity to a ToolSummary.
     *
     * @param tool The Tool entity
     * @return The corresponding ToolSummary
     */
    @Mapping(target = "parameterCount", expression = "java(tool.getParameters().size())")
    @Mapping(target = "dependencyCount", expression = "java(tool.getDependencies().size())")
    @Mapping(target = "categories", expression = "java(mapCategoryNames(tool.getCategories()))")
    ToolSummary toSummary(Tool tool);

    /**
     * Maps a collection of Tool entities to a list of ToolSummary objects.
     *
     * @param tools The Tool entities
     * @return The corresponding list of ToolSummary objects
     */
    List<ToolSummary> toSummaryList(List<Tool> tools);

    /**
     * Maps a collection of Tool entities to a list of ToolDTO objects.
     *
     * @param tools The Tool entities
     * @return The corresponding list of ToolDTO objects
     */
    List<ToolDTO> toDtoList(List<Tool> tools);

    /**
     * Maps a collection of Tool categories to a list of category names.
     *
     * @param categories The Tool categories
     * @return The list of category names
     */
    default List<String> mapCategoryNames(Set<ToolCategory> categories) {
        return categories.stream()
                .map(ToolCategory::getName)
                .collect(Collectors.toList());
    }
}