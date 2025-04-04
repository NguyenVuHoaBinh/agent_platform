package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for transferring API header information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiHeaderDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String value;
    private boolean required;
    private boolean sensitive;
}