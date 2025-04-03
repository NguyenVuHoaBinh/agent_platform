package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a mapping between parameters of dependent tools.
 */
@Entity
@Table(name = "parameter_mapping")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterMapping {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "dependency_id")
    private ToolDependency dependency;

    @Column(name = "source_parameter")
    private String sourceParameter;

    @Column(name = "target_parameter")
    private String targetParameter;
}