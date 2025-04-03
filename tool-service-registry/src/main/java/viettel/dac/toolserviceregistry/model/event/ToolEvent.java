package viettel.dac.toolserviceregistry.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.toolserviceregistry.model.dto.ToolCategoryDTO;
import viettel.dac.toolserviceregistry.model.dto.ToolDependencyDTO;
import viettel.dac.toolserviceregistry.model.dto.ToolParameterDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an event related to tool operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;

    /**
     * ID of the tool
     */
    private String toolId;

    /**
     * Name of the tool
     */
    private String name;

    /**
     * Description of the tool
     */
    private String description;

    /**
     * Whether the tool is active
     */
    private boolean active;

    /**
     * Version of the tool
     */
    private int version;

    /**
     * Parameters of the tool
     */
    private List<ToolParameterDTO> parameters = new ArrayList<>();

    /**
     * Dependencies of the tool
     */
    private List<ToolDependencyDTO> dependencies = new ArrayList<>();

    /**
     * Categories of the tool
     */
    private List<ToolCategoryDTO> categories = new ArrayList<>();

    /**
     * Constructor for ToolEvent.
     *
     * @param eventType The type of the event
     * @param toolId    The ID of the tool
     */
    public ToolEvent(String eventType, String toolId) {
        super(eventType);
        this.toolId = toolId;
    }

    /**
     * Static builder method to create a builder that accounts for parent class
     */
    @Builder
    public static ToolEvent createToolEvent(
            String eventType,
            String toolId,
            String name,
            String description,
            boolean active,
            int version,
            List<ToolParameterDTO> parameters,
            List<ToolDependencyDTO> dependencies,
            List<ToolCategoryDTO> categories) {

        ToolEvent event = new ToolEvent(eventType, toolId);
        event.name = name;
        event.description = description;
        event.active = active;
        event.version = version;
        event.parameters = parameters != null ? parameters : new ArrayList<>();
        event.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        event.categories = categories != null ? categories : new ArrayList<>();
        return event;
    }
}
