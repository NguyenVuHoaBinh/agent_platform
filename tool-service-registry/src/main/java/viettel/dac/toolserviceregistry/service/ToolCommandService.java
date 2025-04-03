package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import viettel.dac.toolserviceregistry.exception.*;
import viettel.dac.toolserviceregistry.model.entity.*;
import viettel.dac.toolserviceregistry.model.event.ToolEvent;
import viettel.dac.toolserviceregistry.model.enums.ToolEventType;
import viettel.dac.toolserviceregistry.model.request.*;
import viettel.dac.toolserviceregistry.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for tool command operations (create, update, delete).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ToolCommandService {
    private final ToolRepository toolRepository;
    private final ToolParameterRepository parameterRepository;
    private final ToolDependencyRepository dependencyRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolExampleRepository exampleRepository;
    private final EventPublisher eventPublisher;
    private final ToolValidator toolValidator;
    private final DependencyValidator dependencyValidator;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.tool-events}")
    private String toolEventsTopic;

    /**
     * Creates a new tool.
     *
     * @param request The tool creation request
     * @return The ID of the created tool
     */
    public String createTool(CreateToolRequest request) {
        log.info("Creating tool: {}", request.getName());

        // Validate tool name uniqueness
        toolRepository.findByName(request.getName())
                .ifPresent(existing -> {
                    throw new DuplicateToolNameException(request.getName());
                });

        // Generate ID if not provided
        String toolId = request.getId() != null ?
                request.getId() : UUID.randomUUID().toString();

        // Create tool entity
        Tool tool = new Tool();
        tool.setId(toolId);
        tool.setName(request.getName());
        tool.setDescription(request.getDescription());
        tool.setActive(true);
        tool.setVersion(1);
        tool.setCreatedAt(LocalDateTime.now());
        tool.setUpdatedAt(LocalDateTime.now());

        // Add parameters
        if (request.getParameters() != null) {
            Set<String> paramNames = new HashSet<>();
            for (ToolParameterRequest paramReq : request.getParameters()) {
                // Check for duplicate parameter names
                if (!paramNames.add(paramReq.getName())) {
                    throw new DuplicateParameterNameException(paramReq.getName());
                }

                ToolParameter param = mapToToolParameter(paramReq);
                param.setId(UUID.randomUUID().toString());
                tool.addParameter(param);
            }
        }

        // Add categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<ToolCategory> categories = categoryRepository.findByIdIn(request.getCategoryIds());
            if (categories.size() != request.getCategoryIds().size()) {
                log.warn("Some category IDs were not found");
            }
            categories.forEach(tool::addCategory);
        }

        // Add examples
        if (request.getExamples() != null) {
            for (ToolExampleRequest exampleReq : request.getExamples()) {
                ToolExample example = mapToToolExample(exampleReq);
                example.setId(UUID.randomUUID().toString());
                tool.addExample(example);
            }
        }

        // Save tool first to establish ID
        Tool savedTool = toolRepository.save(tool);

        // Add dependencies (after tool is saved to have ID)
        if (request.getDependencies() != null && !request.getDependencies().isEmpty()) {
            // Validate dependencies to prevent cycles
            dependencyValidator.validateNoCycles(toolId, request.getDependencies());

            for (ToolDependencyRequest depReq : request.getDependencies()) {
                Tool dependencyTool = toolRepository.findById(depReq.getDependencyToolId())
                        .orElseThrow(() -> new DependencyToolNotFoundException(depReq.getDependencyToolId()));

                ToolDependency dependency = mapToToolDependency(depReq, savedTool, dependencyTool);
                dependency.setId(UUID.randomUUID().toString());

                // Add parameter mappings
                if (depReq.getParameterMappings() != null) {
                    // Validate parameter mappings
                    dependencyValidator.validateParameterMappings(
                            toolId,
                            depReq.getDependencyToolId(),
                            depReq.getParameterMappings());

                    for (ParameterMappingRequest mappingReq : depReq.getParameterMappings()) {
                        ParameterMapping mapping = mapToParameterMapping(mappingReq);
                        mapping.setId(UUID.randomUUID().toString());
                        dependency.addParameterMapping(mapping);
                    }
                }

                savedTool.addDependency(dependency);
            }

            // Save again with dependencies
            savedTool = toolRepository.save(savedTool);
        }

        // Publish tool created event
        publishToolEvent(savedTool, ToolEventType.TOOL_CREATED);

        return toolId;
    }

    /**
     * Updates an existing tool.
     *
     * @param id The ID of the tool to update
     * @param request The tool update request
     * @return The new version of the tool
     */
    public int updateTool(String id, UpdateToolRequest request) {
        log.info("Updating tool: {}", id);

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // Check name uniqueness if changed
        if (!tool.getName().equals(request.getName())) {
            if (toolRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new DuplicateToolNameException(request.getName());
            }
        }

        // Update basic properties
        tool.setName(request.getName());
        tool.setDescription(request.getDescription());
        tool.setUpdatedAt(LocalDateTime.now());
        tool.setVersion(tool.getVersion() + 1);

        // Update parameters (remove all and re-add)
        tool.getParameters().clear();
        if (request.getParameters() != null) {
            Set<String> paramNames = new HashSet<>();
            for (ToolParameterRequest paramReq : request.getParameters()) {
                // Check for duplicate parameter names
                if (!paramNames.add(paramReq.getName())) {
                    throw new DuplicateParameterNameException(paramReq.getName());
                }

                ToolParameter param = mapToToolParameter(paramReq);
                param.setId(paramReq.getId() != null ?
                        paramReq.getId() : UUID.randomUUID().toString());
                tool.addParameter(param);
            }
        }

        // Update categories (remove all and re-add)
        tool.getCategories().clear();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<ToolCategory> categories = categoryRepository.findByIdIn(request.getCategoryIds());
            if (categories.size() != request.getCategoryIds().size()) {
                log.warn("Some category IDs were not found");
            }
            categories.forEach(tool::addCategory);
        }

        // Update examples (remove all and re-add)
        tool.getExamples().clear();
        if (request.getExamples() != null) {
            for (ToolExampleRequest exampleReq : request.getExamples()) {
                ToolExample example = mapToToolExample(exampleReq);
                example.setId(exampleReq.getId() != null ?
                        exampleReq.getId() : UUID.randomUUID().toString());
                tool.addExample(example);
            }
        }

        // Save tool with updated properties, parameters, categories, and examples
        Tool savedTool = toolRepository.save(tool);

        // Update dependencies (remove all and re-add)
        tool.getDependencies().clear();
        toolRepository.save(tool); // Save to remove dependencies first

        if (request.getDependencies() != null && !request.getDependencies().isEmpty()) {
            // Validate dependencies to prevent cycles
            dependencyValidator.validateNoCycles(id, request.getDependencies());

            for (ToolDependencyRequest depReq : request.getDependencies()) {
                Tool dependencyTool = toolRepository.findById(depReq.getDependencyToolId())
                        .orElseThrow(() -> new DependencyToolNotFoundException(depReq.getDependencyToolId()));

                ToolDependency dependency = mapToToolDependency(depReq, savedTool, dependencyTool);
                dependency.setId(depReq.getId() != null ?
                        depReq.getId() : UUID.randomUUID().toString());

                // Add parameter mappings
                if (depReq.getParameterMappings() != null) {
                    // Validate parameter mappings
                    dependencyValidator.validateParameterMappings(
                            id,
                            depReq.getDependencyToolId(),
                            depReq.getParameterMappings());

                    for (ParameterMappingRequest mappingReq : depReq.getParameterMappings()) {
                        ParameterMapping mapping = mapToParameterMapping(mappingReq);
                        mapping.setId(mappingReq.getId() != null ?
                                mappingReq.getId() : UUID.randomUUID().toString());
                        dependency.addParameterMapping(mapping);
                    }
                }

                savedTool.addDependency(dependency);
            }

            // Save again with dependencies
            savedTool = toolRepository.save(savedTool);
        }

        // Publish tool updated event
        publishToolEvent(savedTool, ToolEventType.TOOL_UPDATED);

        return savedTool.getVersion();
    }

    /**
     * Deletes a tool.
     *
     * @param id The ID of the tool to delete
     */
    public void deleteTool(String id) {
        log.info("Deleting tool: {}", id);

        // Check for existing dependents
        long dependentCount = dependencyRepository.countByDependencyToolId(id);
        if (dependentCount > 0) {
            List<ToolDependency> dependents = dependencyRepository.findByDependencyToolId(id);
            List<String> dependentNames = dependents.stream()
                    .map(dep -> dep.getTool().getName())
                    .collect(Collectors.toList());

            throw new ToolHasDependentsException(id, dependentNames);
        }

        // Find existing tool
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        // Publish tool deleted event before deletion
        publishToolEvent(tool, ToolEventType.TOOL_DELETED);

        // Delete the tool
        toolRepository.delete(tool);
    }

    /**
     * Publishes a tool event to Kafka.
     *
     * @param tool The tool entity
     * @param eventType The type of event
     */
    private void publishToolEvent(Tool tool, ToolEventType eventType) {
        ToolEvent event = new ToolEvent();
        event.setToolId(tool.getId());
        event.setEventType(eventType);
        event.setTimestamp(LocalDateTime.now());
        event.setName(tool.getName());
        event.setDescription(tool.getDescription());
        event.setVersion(tool.getVersion());

        eventPublisher.publish(toolEventsTopic, tool.getId(), event);
    }

    /**
     * Maps a ToolParameterRequest to a ToolParameter entity.
     *
     * @param request The parameter request
     * @return The parameter entity
     */
    private ToolParameter mapToToolParameter(ToolParameterRequest request) {
        ToolParameter parameter = new ToolParameter();
        parameter.setName(request.getName());
        parameter.setDescription(request.getDescription());
        parameter.setParameterType(request.getParameterType());
        parameter.setRequired(request.isRequired());
        parameter.setDefaultValue(request.getDefaultValue());
        parameter.setValidationPattern(request.getValidationPattern());
        parameter.setValidationMessage(request.getValidationMessage());
        parameter.setConditionalOn(request.getConditionalOn());
        parameter.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        parameter.setExamples(request.getExamples());
        parameter.setSuggestionQuery(request.getSuggestionQuery());
        return parameter;
    }

    /**
     * Maps a ToolDependencyRequest to a ToolDependency entity.
     *
     * @param request The dependency request
     * @param tool The tool entity
     * @param dependencyTool The dependency tool entity
     * @return The dependency entity
     */
    private ToolDependency mapToToolDependency(
            ToolDependencyRequest request, Tool tool, Tool dependencyTool) {
        ToolDependency dependency = new ToolDependency();
        dependency.setTool(tool);
        dependency.setDependencyTool(dependencyTool);
        dependency.setDependencyType(request.getDependencyType());
        dependency.setDescription(request.getDescription());
        return dependency;
    }

    /**
     * Maps a ParameterMappingRequest to a ParameterMapping entity.
     *
     * @param request The parameter mapping request
     * @return The parameter mapping entity
     */
    private ParameterMapping mapToParameterMapping(ParameterMappingRequest request) {
        ParameterMapping mapping = new ParameterMapping();
        mapping.setSourceParameter(request.getSourceParameter());
        mapping.setTargetParameter(request.getTargetParameter());
        return mapping;
    }

    /**
     * Maps a ToolExampleRequest to a ToolExample entity.
     *
     * @param request The example request
     * @return The example entity
     */
    private ToolExample mapToToolExample(ToolExampleRequest request) {
        ToolExample example = new ToolExample();
        example.setInputText(request.getInputText());
        try {
            example.setOutputParameters(objectMapper.writeValueAsString(request.getOutputParameters()));
        } catch (Exception e) {
            log.error("Error serializing output parameters", e);
            example.setOutputParameters("{}");
        }
        return example;
    }
}