package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.toolserviceregistry.exception.ToolNotFoundException;
import viettel.dac.toolserviceregistry.model.dto.*;
import viettel.dac.toolserviceregistry.model.entity.*;
import viettel.dac.toolserviceregistry.model.enums.ToolType;
import viettel.dac.toolserviceregistry.model.reponse.*;
import viettel.dac.toolserviceregistry.repository.ApiToolMetadataRepository;
import viettel.dac.toolserviceregistry.repository.ToolDependencyRepository;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ToolQueryService {
    private final ToolRepository toolRepository;
    private final ToolDependencyRepository dependencyRepository;
    private final ApiToolMetadataRepository apiToolMetadataRepository;
    private final ApiToolService apiToolService;
    private final ToolDependencyGraphService graphService;
    private final ExecutionPlanService executionPlanService;
    private final ObjectMapper objectMapper;

    public ToolQueryResponse queryTools(
            Boolean active,
            String category,
            ToolType toolType,
            String search,
            int page,
            int size,
            String sort) {

        log.info("Querying tools with filters: active={}, category={}, toolType={}, search={}",
                active, category, toolType, search);

        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        // Create pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // Apply filters
        Specification<Tool> spec = Specification.where(null);

        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }

        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Join<Tool, ToolCategory> categoryJoin = root.join("categories");
                return cb.equal(categoryJoin.get("name"), category);
            });
        }

        // Add filter for tool type
        if (toolType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("toolType"), toolType));
        }

        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%")
            ));
        }

        // Execute query
        Page<Tool> toolPage = toolRepository.findAll(spec, pageable);

        // Map to response
        List<ToolSummary> content = toolPage.getContent().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());

        return ToolQueryResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(toolPage.getTotalElements())
                .totalPages(toolPage.getTotalPages())
                .build();
    }

    public ToolDetailResponse getToolById(String id) {
        log.info("Fetching tool details for id: {}", id);

        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        return mapToDetailResponse(tool);
    }

    public ToolDetailResponse getToolByName(String name) {
        log.info("Fetching tool details for name: {}", name);

        Tool tool = toolRepository.findByName(name)
                .orElseThrow(() -> new ToolNotFoundException("name: " + name));

        return mapToDetailResponse(tool);
    }

    public List<ToolDependencyView> getToolDependencies(String id) {
        log.info("Fetching dependencies for tool: {}", id);

        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new ToolNotFoundException(id));

        return tool.getDependencies().stream()
                .map(this::mapToDependencyView)
                .collect(Collectors.toList());
    }

    public List<ToolDependencyView> getToolDependents(String id) {
        log.info("Fetching dependents for tool: {}", id);

        List<ToolDependency> dependents = dependencyRepository.findByDependencyToolId(id);

        return dependents.stream()
                .map(this::mapToDependentView)
                .collect(Collectors.toList());
    }

    public ExecutionPlanView generateExecutionPlan(
            List<String> toolIds,
            Map<String, Object> providedParameters) {
        log.info("Generating execution plan for tools: {}", toolIds);

        return executionPlanService.generateExecutionPlan(toolIds, providedParameters);
    }

    private ToolSummary mapToSummary(Tool tool) {
        return ToolSummary.builder()
                .id(tool.getId())
                .name(tool.getName())
                .description(tool.getDescription())
                .active(tool.isActive())
                .version(tool.getVersion())
                .updatedAt(tool.getUpdatedAt())
                .parameterCount(tool.getParameters().size())
                .dependencyCount(tool.getDependencies().size())
                .toolType(tool.getToolType()) // Include tool type
                .categories(tool.getCategories().stream()
                        .map(ToolCategory::getName)
                        .collect(Collectors.toList()))
                .build();
    }

    private ToolDetailResponse mapToDetailResponse(Tool tool) {
        ToolDetailResponse response = ToolDetailResponse.builder()
                .id(tool.getId())
                .name(tool.getName())
                .description(tool.getDescription())
                .active(tool.isActive())
                .version(tool.getVersion())
                .createdAt(tool.getCreatedAt())
                .updatedAt(tool.getUpdatedAt())
                .toolType(tool.getToolType()) // Include tool type
                .parameters(tool.getParameters().stream()
                        .map(this::mapToParameterDTO)
                        .collect(Collectors.toList()))
                .dependencies(tool.getDependencies().stream()
                        .map(this::mapToDependencyDTO)
                        .collect(Collectors.toList()))
                .categories(tool.getCategories().stream()
                        .map(this::mapToCategoryDTO)
                        .collect(Collectors.toList()))
                .examples(tool.getExamples().stream()
                        .map(this::mapToExampleDTO)
                        .collect(Collectors.toList()))
                .build();

        // Add API metadata if this is an API tool
        if (tool.getToolType() == ToolType.API_TOOL) {
            apiToolMetadataRepository.findByToolId(tool.getId())
                    .ifPresent(apiMetadata -> {
                        // Fixed: Using a public method or directly getting the metadata
                        response.setApiMetadata(apiToolService.getApiToolMetadataDTO(tool.getId()));
                    });
        }

        return response;
    }

    private ToolDependencyView mapToDependencyView(ToolDependency dependency) {
        // Create a new ArrayList to ensure we're using java.util.List
        List<ParameterMappingView> mappings = new ArrayList<>();

        for (ParameterMapping mapping : dependency.getParameterMappings()) {
            mappings.add(ParameterMappingView.builder()
                    .sourceParameter(mapping.getSourceParameter())
                    .targetParameter(mapping.getTargetParameter())
                    .build());
        }

        return ToolDependencyView.builder()
                .toolId(dependency.getTool().getId())
                .toolName(dependency.getTool().getName())
                .dependencyToolId(dependency.getDependencyTool().getId())
                .dependencyToolName(dependency.getDependencyTool().getName())
                .dependencyType(dependency.getDependencyType().toString())
                .description(dependency.getDescription())
                .parameterMappings(mappings) // Fixed: Using the correct List type
                .build();
    }

    private ToolDependencyView mapToDependentView(ToolDependency dependency) {
        // Create a new ArrayList to ensure we're using java.util.List
        List<ParameterMappingView> mappings = new ArrayList<>();

        for (ParameterMapping mapping : dependency.getParameterMappings()) {
            mappings.add(ParameterMappingView.builder()
                    .sourceParameter(mapping.getSourceParameter())
                    .targetParameter(mapping.getTargetParameter())
                    .build());
        }

        return ToolDependencyView.builder()
                .toolId(dependency.getDependencyTool().getId())
                .toolName(dependency.getDependencyTool().getName())
                .dependencyToolId(dependency.getTool().getId())
                .dependencyToolName(dependency.getTool().getName())
                .dependencyType(dependency.getDependencyType().toString())
                .description(dependency.getDescription())
                .parameterMappings(mappings) // Fixed: Using the correct List type
                .build();
    }

    private ToolParameterDTO mapToParameterDTO(ToolParameter parameter) {
        return ToolParameterDTO.builder()
                .id(parameter.getId())
                .name(parameter.getName())
                .description(parameter.getDescription())
                .parameterType(parameter.getParameterType())
                .required(parameter.isRequired())
                .defaultValue(parameter.getDefaultValue())
                .validationPattern(parameter.getValidationPattern())
                .validationMessage(parameter.getValidationMessage())
                .conditionalOn(parameter.getConditionalOn())
                .priority(parameter.getPriority())
                .examples(parameter.getExamples())
                .suggestionQuery(parameter.getSuggestionQuery())
                .build();
    }

    private ToolDependencyDTO mapToDependencyDTO(ToolDependency dependency) {
        return ToolDependencyDTO.builder()
                .dependencyToolId(dependency.getDependencyTool().getId())
                .dependencyToolName(dependency.getDependencyTool().getName())
                .dependencyType(dependency.getDependencyType())
                .parameterMappings(dependency.getParameterMappings().stream()
                        .map(this::mapToParameterMappingDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    private ParameterMappingDTO mapToParameterMappingDTO(ParameterMapping mapping) {
        return ParameterMappingDTO.builder()
                .id(mapping.getId())
                .sourceParameter(mapping.getSourceParameter())
                .targetParameter(mapping.getTargetParameter())
                .build();
    }

    private ToolCategoryDTO mapToCategoryDTO(ToolCategory category) {
        return ToolCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    private ToolExampleDTO mapToExampleDTO(ToolExample example) {
        Map<String, Object> outputParams = new HashMap<>();
        try {
            outputParams = objectMapper.readValue(example.getOutputParameters(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error parsing output parameters for example: {}", example.getId(), e);
        }

        return ToolExampleDTO.builder()
                .id(example.getId())
                .inputText(example.getInputText())
                .outputParameters(outputParams)
                .build();
    }
}