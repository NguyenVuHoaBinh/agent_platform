package viettel.dac.toolserviceregistry.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import viettel.dac.toolserviceregistry.model.enums.ToolType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for tool type operations.
 */
@RestController
@RequestMapping("/tools/types")
@RequiredArgsConstructor
public class ToolTypeController {

    /**
     * Gets all available tool types.
     *
     * @return List of available tool types with descriptions
     */
    @GetMapping
    public List<Map<String, String>> getToolTypes() {
        return Arrays.stream(ToolType.values())
                .map(type -> {
                    Map<String, String> typeInfo = new HashMap<>();
                    typeInfo.put("name", type.name());
                    typeInfo.put("description", getToolTypeDescription(type));
                    return typeInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets a description for a tool type.
     *
     * @param type The tool type
     * @return The description of the tool type
     */
    private String getToolTypeDescription(ToolType type) {
        switch (type) {
            case API_TOOL:
                return "Tools that interact with external APIs";
            case OTHER:
                return "Other types of tools";
            default:
                return "Unknown tool type";
        }
    }
}