package viettel.dac.toolserviceregistry.model.reponse;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for tool creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCreatedResponse {
    private String id;
    private String name;
    private String message;
}
