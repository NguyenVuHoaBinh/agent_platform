package viettel.dac.toolserviceregistry.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import viettel.dac.toolserviceregistry.model.dto.ToolExampleDTO;
import viettel.dac.toolserviceregistry.model.entity.ToolExample;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper for ToolExample entities and DTOs.
 */
@Mapper(componentModel = "spring", imports = {ObjectMapper.class, HashMap.class, Map.class, JsonProcessingException.class})
public interface ToolExampleMapper {

    /**
     * Maps a ToolExample entity to a ToolExampleDTO.
     *
     * @param example The ToolExample entity
     * @return The corresponding ToolExampleDTO
     */
    @Mapping(target = "outputParameters", expression = "java(parseOutputParameters(example.getOutputParameters()))")
    ToolExampleDTO toDto(ToolExample example);

    /**
     * Maps a list of ToolExample entities to a list of ToolExampleDTO objects.
     *
     * @param examples The ToolExample entities
     * @return The corresponding list of ToolExampleDTO objects
     */
    List<ToolExampleDTO> toDtoList(List<ToolExample> examples);

    /**
     * Parses the JSON string of output parameters into a Map.
     *
     * @param outputParameters The JSON string of output parameters
     * @return The parsed Map of output parameters
     */
    default Map<String, Object> parseOutputParameters(String outputParameters) {
        try {
            return new ObjectMapper().readValue(outputParameters, HashMap.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
}
