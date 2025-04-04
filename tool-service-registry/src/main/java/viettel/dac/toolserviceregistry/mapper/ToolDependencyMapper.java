package viettel.dac.toolserviceregistry.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import viettel.dac.toolserviceregistry.model.dto.ToolDependencyDTO;
import viettel.dac.toolserviceregistry.model.entity.ToolDependency;
import viettel.dac.toolserviceregistry.model.reponse.ToolDependencyView;

import java.util.List;

/**
 * Mapper for ToolDependency entities and DTOs.
 */
@Mapper(componentModel = "spring", uses = {ParameterMappingMapper.class})
public interface ToolDependencyMapper {

    /**
     * Maps a ToolDependency entity to a ToolDependencyDTO.
     *
     * @param dependency The ToolDependency entity
     * @return The corresponding ToolDependencyDTO
     */
    @Mapping(target = "dependencyToolId", source = "dependencyTool.id")
    @Mapping(target = "dependencyToolName", source = "dependencyTool.name")
    ToolDependencyDTO toDto(ToolDependency dependency);

    /**
     * Maps a list of ToolDependency entities to a list of ToolDependencyDTO objects.
     *
     * @param dependencies The ToolDependency entities
     * @return The corresponding list of ToolDependencyDTO objects
     */
    List<ToolDependencyDTO> toDtoList(List<ToolDependency> dependencies);

    /**
     * Maps a ToolDependency entity to a ToolDependencyView.
     *
     * @param dependency The ToolDependency entity
     * @return The corresponding ToolDependencyView
     */
    @Mapping(target = "toolId", source = "tool.id")
    @Mapping(target = "toolName", source = "tool.name")
    @Mapping(target = "dependencyToolId", source = "dependencyTool.id")
    @Mapping(target = "dependencyToolName", source = "dependencyTool.name")
    @Mapping(target = "dependencyType", source = "dependencyType")
    @Mapping(target = "parameterMappings", source = "parameterMappings")
    ToolDependencyView toView(ToolDependency dependency);

    /**
     * Maps a list of ToolDependency entities to a list of ToolDependencyView objects.
     *
     * @param dependencies The ToolDependency entities
     * @return The corresponding list of ToolDependencyView objects
     */
    List<ToolDependencyView> toViewList(List<ToolDependency> dependencies);
}
