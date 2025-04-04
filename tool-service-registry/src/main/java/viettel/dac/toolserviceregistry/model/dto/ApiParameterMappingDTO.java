package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ApiParameterLocation;

import java.io.Serializable;

/**
 * DTO for transferring API parameter mapping data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiParameterMappingDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String toolParameterId;
    private ApiParameterLocation apiLocation;
    private String apiParameterName;
    private boolean requiredForApi;
    private String transformationExpression;
    private String responseExtractionPath;
}