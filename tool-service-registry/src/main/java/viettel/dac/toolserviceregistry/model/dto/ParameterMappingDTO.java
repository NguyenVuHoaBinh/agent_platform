package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * DTO for transferring parameter mapping data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterMappingDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String sourceParameter;
    private String targetParameter;
}