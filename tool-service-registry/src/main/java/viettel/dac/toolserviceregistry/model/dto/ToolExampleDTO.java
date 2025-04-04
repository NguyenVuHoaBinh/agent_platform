package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for transferring tool example data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExampleDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String inputText;

    @Builder.Default
    private Map<String, Object> outputParameters = new HashMap<>();
}