package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request model for generating an execution plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlanRequest {
    /**
     * Required list of tool IDs to include in the plan.
     */
    @NotEmpty(message = "At least one tool ID is required")
    @Builder.Default
    private List<String> toolIds = new ArrayList<>();

    /**
     * Optional map of parameters that are already available.
     */
    @Builder.Default
    private Map<String, Object> providedParameters = new HashMap<>();
}