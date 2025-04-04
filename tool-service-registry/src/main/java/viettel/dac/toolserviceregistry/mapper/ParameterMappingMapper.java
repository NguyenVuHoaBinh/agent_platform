package viettel.dac.toolserviceregistry.mapper;

import org.mapstruct.Mapper;
import viettel.dac.toolserviceregistry.model.dto.ParameterMappingDTO;
import viettel.dac.toolserviceregistry.model.reponse.ParameterMappingView;
import viettel.dac.toolserviceregistry.model.entity.ParameterMapping;
import java.util.List;

/**
 * Mapper for ParameterMapping entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface ParameterMappingMapper {

    /**
     * Maps a ParameterMapping entity to a ParameterMappingDTO.
     *
     * @param mapping The ParameterMapping entity
     * @return The corresponding ParameterMappingDTO
     */
    ParameterMappingDTO toDto(ParameterMapping mapping);

    /**
     * Maps a list of ParameterMapping entities to a list of ParameterMappingDTO objects.
     *
     * @param mappings The ParameterMapping entities
     * @return The corresponding list of ParameterMappingDTO objects
     */
    List<ParameterMappingDTO> toDtoList(List<ParameterMapping> mappings);

    /**
     * Maps a ParameterMapping entity to a ParameterMappingView.
     *
     * @param mapping The ParameterMapping entity
     * @return The corresponding ParameterMappingView
     */
    ParameterMappingView toView(ParameterMapping mapping);

    /**
     * Maps a list of ParameterMapping entities to a list of ParameterMappingView objects.
     *
     * @param mappings The ParameterMapping entities
     * @return The corresponding list of ParameterMappingView objects
     */
    List<ParameterMappingView> toViewList(List<ParameterMapping> mappings);
}