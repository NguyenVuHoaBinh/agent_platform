package viettel.dac.toolserviceregistry.model.reponse;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * View of a parameter mapping for dependency relationships.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterMappingView {
    private String sourceParameter;
    private String targetParameter;
}
