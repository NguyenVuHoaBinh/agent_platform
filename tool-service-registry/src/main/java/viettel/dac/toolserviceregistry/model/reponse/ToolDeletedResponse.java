package viettel.dac.toolserviceregistry.model.reponse;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for tool deletion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDeletedResponse {
    private String id;
    private String message;
}
