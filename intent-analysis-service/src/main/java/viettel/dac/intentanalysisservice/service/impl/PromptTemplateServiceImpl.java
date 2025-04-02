package viettel.dac.intentanalysisservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import viettel.dac.intentanalysisservice.dto.ToolDTO;
import viettel.dac.intentanalysisservice.model.Intent;
import viettel.dac.intentanalysisservice.service.PromptTemplateService;
import viettel.dac.intentanalysisservice.util.JsonUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of PromptTemplateService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTemplateServiceImpl implements PromptTemplateService {

    @Value("classpath:prompts/intent-analysis-prompt.txt")
    private Resource intentAnalysisPromptTemplate;

    @Value("classpath:prompts/parameter-extraction-prompt.txt")
    private Resource parameterExtractionPromptTemplate;

    private final JsonUtil jsonUtil;

    @Override
    public String createIntentAnalysisPrompt(String userInput, List<ToolDTO> tools) {
        try {
            String template = loadTemplateContent(intentAnalysisPromptTemplate);

            // Format tools
            String formattedTools = formatToolsForPrompt(tools);

            // Replace placeholders
            String prompt = template
                    .replace("{{TOOLS}}", formattedTools)
                    .replace("{{USER_INPUT}}", userInput);

            return prompt;
        } catch (IOException e) {
            log.error("Error creating intent analysis prompt", e);
            // Fallback to a basic prompt
            return createBasicIntentAnalysisPrompt(userInput, tools);
        }
    }

    @Override
    public String createParameterExtractionPrompt(String userInput, List<Intent> intents, List<ToolDTO> tools) {
        try {
            String template = loadTemplateContent(parameterExtractionPromptTemplate);

            // Format intents
            String formattedIntents = jsonUtil.toJson(intents);

            // Format tool parameters
            String formattedToolParameters = formatToolParametersForPrompt(intents, tools);

            // Replace placeholders
            String prompt = template
                    .replace("{{INTENTS}}", formattedIntents)
                    .replace("{{TOOL_PARAMETERS}}", formattedToolParameters)
                    .replace("{{USER_INPUT}}", userInput);

            return prompt;
        } catch (IOException e) {
            log.error("Error creating parameter extraction prompt", e);
            // Fallback to a basic prompt
            return createBasicParameterExtractionPrompt(userInput, intents, tools);
        }
    }

    /**
     * Load template content from a resource file.
     *
     * @param resource The resource to load
     * @return The template content as a string
     * @throws IOException If the resource cannot be read
     */
    private String loadTemplateContent(Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    /**
     * Format the tools information for the prompt.
     *
     * @param tools List of tools
     * @return Formatted tools string
     */
    private String formatToolsForPrompt(List<ToolDTO> tools) {
        return tools.stream()
                .map(tool -> "- " + tool.getName() + ": " + tool.getDescription())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Format the tool parameters information for the prompt.
     *
     * @param intents List of intents
     * @param tools List of tools
     * @return Formatted tool parameters string
     */
    private String formatToolParametersForPrompt(List<Intent> intents, List<ToolDTO> tools) {
        StringBuilder sb = new StringBuilder();

        for (Intent intent : intents) {
            // Find the matching tool
            tools.stream()
                    .filter(tool -> tool.getName().equals(intent.getIntent()))
                    .findFirst()
                    .ifPresent(tool -> {
                        sb.append("Tool: ").append(tool.getName()).append("\n");
                        sb.append("Description: ").append(tool.getDescription()).append("\n");
                        sb.append("Parameters:\n");

                        if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
                            tool.getParameters().forEach(param -> {
                                sb.append("  - ").append(param.getName());
                                sb.append(" (").append(param.getParameterType()).append("): ");
                                sb.append(param.getDescription());
                                if (param.isRequired()) {
                                    sb.append(" [Required]");
                                }
                                sb.append("\n");
                            });
                        } else {
                            sb.append("  No parameters\n");
                        }

                        sb.append("\n");
                    });
        }

        return sb.toString();
    }

    /**
     * Create a basic intent analysis prompt as a fallback.
     *
     * @param userInput The user's input text
     * @param tools List of available tools
     * @return A basic prompt for intent analysis
     */
    private String createBasicIntentAnalysisPrompt(String userInput, List<ToolDTO> tools) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("You are an intent classifier for a conversational agent. ")
                .append("Analyze the user input and identify which intents are present from the available tools. ")
                .append("Respond with a JSON array of intents.\n\n");

        // Add the available tools
        promptBuilder.append("Available tools:\n");
        promptBuilder.append(formatToolsForPrompt(tools));

        // Add the user input
        promptBuilder.append("\nUser input: \"").append(userInput).append("\"\n\n");

        // Add the expected format
        promptBuilder.append("Respond with a JSON array of objects. Each object should have an 'intent' field matching one of the tool names, ")
                .append("and a 'confidence' field with a value between 0 and 1. ")
                .append("If no intents match, respond with [{\"intent\": \"default_action\", \"confidence\": 1.0}].\n")
                .append("Format: [{\"intent\": \"tool_name\", \"confidence\": 0.95}, ...]\n");

        return promptBuilder.toString();
    }

    /**
     * Create a basic parameter extraction prompt as a fallback.
     *
     * @param userInput The user's input text
     * @param intents List of identified intents
     * @param tools List of tools with parameter definitions
     * @return A basic prompt for parameter extraction
     */
    private String createBasicParameterExtractionPrompt(String userInput, List<Intent> intents, List<ToolDTO> tools) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("You are a parameter extractor for a conversational agent. ")
                .append("For each identified intent, extract the required parameters from the user input. ")
                .append("Respond with a JSON array of intents with their parameters.\n\n");

        // List the identified intents
        promptBuilder.append("Identified intents: ");
        promptBuilder.append(jsonUtil.toJson(intents)).append("\n\n");

        // Add the tool descriptions with parameters
        promptBuilder.append("Tool descriptions and parameters:\n");
        promptBuilder.append(formatToolParametersForPrompt(intents, tools));

        // Add the user input
        promptBuilder.append("User input: \"").append(userInput).append("\"\n\n");

        // Add the expected format
        promptBuilder.append("For each intent, extract the parameter values from the user input. ")
                .append("Respond with a JSON array of intents with their parameters. ")
                .append("Format: [{\"intent\": \"tool_name\", \"parameters\": {\"param1\": \"value1\", ...}, \"confidence\": 0.95, \"state\": 0}, ...]\n");

        return promptBuilder.toString();
    }
}
