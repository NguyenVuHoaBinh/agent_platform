package viettel.dac.intentanalysisservice.service;

import viettel.dac.intentanalysisservice.dto.ToolDTO;
import java.util.List;

/**
 * Service for interacting with the Tool Registry Service.
 */
public interface ToolService {

    /**
     * Get tools by their IDs.
     *
     * @param toolIds List of tool IDs to fetch (if null or empty, returns all tools)
     * @return List of tools
     */
    List<ToolDTO> getTools(List<String> toolIds);

    /**
     * Get tools by their names.
     *
     * @param toolNames List of tool names to fetch
     * @return List of tools
     */
    List<ToolDTO> getToolsByNames(List<String> toolNames);

    /**
     * Get all active tools.
     *
     * @return List of all active tools
     */
    List<ToolDTO> getAllTools();

    /**
     * Get a tool by its ID.
     *
     * @param toolId The tool ID
     * @return The tool, or null if not found
     */
    ToolDTO getToolById(String toolId);

    /**
     * Get a tool by its name.
     *
     * @param name The tool name
     * @return The tool, or null if not found
     */
    ToolDTO getToolByName(String name);
}
