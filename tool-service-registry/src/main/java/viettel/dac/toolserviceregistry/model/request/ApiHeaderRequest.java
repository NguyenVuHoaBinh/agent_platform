package viettel.dac.toolserviceregistry.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for creating or updating an API header.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiHeaderRequest {
    private String id;

    @NotBlank(message = "Header name is required")
    private String name;

    private String value;

    private boolean required;

    private boolean sensitive;
}