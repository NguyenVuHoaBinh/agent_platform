package viettel.dac.toolserviceregistry.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO representing the result of parameter validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean valid;
    private String message;
}