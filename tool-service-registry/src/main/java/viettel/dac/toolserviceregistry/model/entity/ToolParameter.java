package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ParameterSource;
import viettel.dac.toolserviceregistry.model.enums.ParameterType;

/**
 * Enhanced entity representing a parameter for a tool.
 */
@Entity
@Table(name = "tool_parameter")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameter {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "tool_id")
    private Tool tool;

    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "parameter_type")
    private ParameterType parameterType;

    private boolean required;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "validation_pattern")
    private String validationPattern;

    @Column(name = "validation_message")
    private String validationMessage;

    @Column(name = "conditional_on")
    private String conditionalOn;

    @Column(name = "priority")
    private int priority;

    @Column(name = "examples", length = 1000)
    private String examples;

    @Column(name = "suggestion_query")
    private String suggestionQuery;

    // New fields

    @Enumerated(EnumType.STRING)
    @Column(name = "parameter_source")
    private ParameterSource parameterSource = ParameterSource.USER_INPUT;

    @Column(name = "min_value")
    private String minValue;

    @Column(name = "max_value")
    private String maxValue;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "allowed_values", length = 1000)
    private String allowedValues;

    @Column(name = "format_hint")
    private String formatHint;

    @Column(name = "is_sensitive")
    private boolean sensitive;

    @Column(name = "is_array")
    private boolean isArray;

    @Column(name = "array_item_type")
    private String arrayItemType;

    @Column(name = "object_schema", length = 2000)
    private String objectSchema;

    @Column(name = "extraction_path")
    private String extractionPath;
}