package viettel.dac.toolserviceregistry.mapper;

import org.mapstruct.Mapper;
import viettel.dac.toolserviceregistry.model.dto.ToolParameterDTO;
import viettel.dac.toolserviceregistry.model.entity.ToolParameter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Mapper for ToolParameter entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface ToolParameterMapper {

    /**
     * Maps a ToolParameter entity to a ToolParameterDTO.
     *
     * @param parameter The ToolParameter entity
     * @return The corresponding ToolParameterDTO
     */
    ToolParameterDTO toDto(ToolParameter parameter);

    /**
     * Maps a list of ToolParameter entities to a list of ToolParameterDTO objects.
     *
     * @param parameters The ToolParameter entities
     * @return The corresponding list of ToolParameterDTO objects
     */
    List<ToolParameterDTO> toDtoList(List<ToolParameter> parameters);

    /**
     * Converts a comma-separated string to a list of strings.
     *
     * @param allowedValues The comma-separated string
     * @return List of strings
     */
    default List<String> map(String allowedValues) {
        if (allowedValues == null || allowedValues.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(allowedValues.split(","));
    }
}