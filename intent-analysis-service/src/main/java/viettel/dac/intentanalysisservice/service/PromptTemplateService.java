package viettel.dac.intentanalysisservice.service;

import viettel.dac.intentanalysisservice.dto.ToolDTO;
import viettel.dac.intentanalysisservice.model.Intent;

import java.util.List;

/**
 * Service for creating prompt templates for LLM.
 */
public interface PromptTemplateService {

    /**
     * Create a prompt for intent analysis.
     *
     * @param userInput The user's input text to analyze
     * @param tools List of available tools to consider
     * @return A formatted prompt for the LLM
     */
    String createIntentAnalysisPrompt(String userInput, List<ToolDTO> tools);

    /**
     * Create a prompt for parameter extraction.
     *
     * @param userInput The user's input text
     * @param intents List of intents identified in the analysis step
     * @param tools List of tools with parameter definitions
     * @return A formatted prompt for the LLM
     */
    String createParameterExtractionPrompt(String userInput, List<Intent> intents, List<ToolDTO> tools);
}