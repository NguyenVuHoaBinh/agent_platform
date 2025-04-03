package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an example usage of a tool.
 */
@Entity
@Table(name = "tool_example")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExample {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "tool_id")
    private Tool tool;

    @Column(name = "input_text", length = 1000)
    private String inputText;

    @Column(name = "output_parameters", columnDefinition = "json")
    private String outputParameters;
}