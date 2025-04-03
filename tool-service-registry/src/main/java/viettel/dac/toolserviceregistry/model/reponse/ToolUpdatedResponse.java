package viettel.dac.toolserviceregistry.model.reponse;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for tool update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolUpdatedResponse {
    private String id;
    private String name;
    private int version;
    private String message;
}