package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.DependencyType;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a dependency between tools.
 */
@Entity
@Table(name = "tool_dependency")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDependency {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "tool_id")
    private Tool tool;

    @ManyToOne
    @JoinColumn(name = "dependency_tool_id")
    private Tool dependencyTool;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type")
    private DependencyType dependencyType;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "dependency", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParameterMapping> parameterMappings = new ArrayList<>();

    /**
     * Adds a parameter mapping to the dependency
     * @param mapping The parameter mapping to add
     */
    public void addParameterMapping(ParameterMapping mapping) {
        parameterMappings.add(mapping);
        mapping.setDependency(this);
    }
}