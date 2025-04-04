package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.enums.ToolType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a tool in the registry.
 */
@Entity
@Table(name = "tool")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tool {
    @Id
    private String id;

    @Column(unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    private boolean active;

    private int version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ToolParameter> parameters = new ArrayList<>();

    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ToolDependency> dependencies = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type")
    private ToolType toolType = ToolType.OTHER;

    @ManyToMany
    @JoinTable(
            name = "tool_category_mapping",
            joinColumns = @JoinColumn(name = "tool_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<ToolCategory> categories = new HashSet<>();

    @OneToMany(mappedBy = "tool", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ToolExample> examples = new ArrayList<>();

    /**
     * Adds a parameter to the tool
     * @param parameter The parameter to add
     */
    public void addParameter(ToolParameter parameter) {
        parameters.add(parameter);
        parameter.setTool(this);
    }

    /**
     * Adds a dependency to the tool
     * @param dependency The dependency to add
     */
    public void addDependency(ToolDependency dependency) {
        dependencies.add(dependency);
        dependency.setTool(this);
    }

    /**
     * Adds a category to the tool
     * @param category The category to add
     */
    public void addCategory(ToolCategory category) {
        categories.add(category);
    }

    /**
     * Adds an example to the tool
     * @param example The example to add
     */
    public void addExample(ToolExample example) {
        examples.add(example);
        example.setTool(this);
    }
}