package viettel.dac.toolserviceregistry.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a category for grouping tools.
 */
@Entity
@Table(name = "tool_category")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCategory {
    @Id
    private String id;

    @Column(unique = true)
    private String name;

    private String description;

    @ManyToMany(mappedBy = "categories")
    @Builder.Default
    private Set<Tool> tools = new HashSet<>();
}