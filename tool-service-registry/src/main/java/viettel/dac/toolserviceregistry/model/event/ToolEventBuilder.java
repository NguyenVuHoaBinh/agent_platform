package viettel.dac.toolserviceregistry.model.event;

import viettel.dac.toolserviceregistry.model.dto.ToolCategoryDTO;
import viettel.dac.toolserviceregistry.model.dto.ToolDependencyDTO;
import viettel.dac.toolserviceregistry.model.dto.ToolParameterDTO;
import viettel.dac.toolserviceregistry.model.event.ToolEvent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Builder pattern implementation for ToolEvent.
 */
public class ToolEventBuilder {
    private String eventType;
    private String toolId;
    private String name;
    private String description;
    private boolean active;
    private int version;
    private List<ToolParameterDTO> parameters = new ArrayList<>();
    private List<ToolDependencyDTO> dependencies = new ArrayList<>();
    private List<ToolCategoryDTO> categories = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public ToolEventBuilder eventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public ToolEventBuilder toolId(String toolId) {
        this.toolId = toolId;
        return this;
    }

    public ToolEventBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ToolEventBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ToolEventBuilder active(boolean active) {
        this.active = active;
        return this;
    }

    public ToolEventBuilder version(int version) {
        this.version = version;
        return this;
    }

    public ToolEventBuilder parameters(List<ToolParameterDTO> parameters) {
        this.parameters = parameters;
        return this;
    }

    public ToolEventBuilder dependencies(List<ToolDependencyDTO> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public ToolEventBuilder categories(List<ToolCategoryDTO> categories) {
        this.categories = categories;
        return this;
    }

    public ToolEventBuilder metadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public ToolEventBuilder addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public ToolEvent build() {
        ToolEvent event = new ToolEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setTimestamp(LocalDateTime.now());
        event.setToolId(toolId);
        event.setName(name);
        event.setDescription(description);
        event.setActive(active);
        event.setVersion(version);
        event.setParameters(parameters);
        event.setDependencies(dependencies);
        event.setCategories(categories);
        event.setMetadata(metadata);
        return event;
    }

    /**
     * Creates a new builder for ToolEvent.
     *
     * @return A new ToolEventBuilder
     */
    public static ToolEventBuilder builder() {
        return new ToolEventBuilder();
    }
}
