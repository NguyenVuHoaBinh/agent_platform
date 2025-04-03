package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for transferring tool example data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExampleDTO {
    private String id;
    private String inputText;

    @Builder.Default
    private Map<String, Object> outputParameters = new HashMap<>();
}