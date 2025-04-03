package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a parameter for a tool.
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

    @Column(name = "parameter_type")
    private String parameterType;

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
}