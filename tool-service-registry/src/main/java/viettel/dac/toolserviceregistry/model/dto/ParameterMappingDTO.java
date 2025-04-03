package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring parameter mapping data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterMappingDTO {
    private String id;
    private String sourceParameter;
    private String targetParameter;
}