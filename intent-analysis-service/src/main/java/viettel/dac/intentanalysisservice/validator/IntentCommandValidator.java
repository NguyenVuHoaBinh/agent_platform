package viettel.dac.intentanalysisservice.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import viettel.dac.intentanalysisservice.model.AnalyzeIntentCommand;
import viettel.dac.intentanalysisservice.model.ExtractParametersCommand;

/**
 * Validator for intent analysis commands.
 */
@Component
@Slf4j
public class IntentCommandValidator {

    /**
     * Validate an analyze intent command.
     *
     * @param command The command to validate
     * @throws IllegalArgumentException if the command is invalid
     */
    public void validateAnalyzeIntent(AnalyzeIntentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        if (!StringUtils.hasText(command.getUserInput())) {
            throw new IllegalArgumentException("User input is required");
        }

        if (!StringUtils.hasText(command.getSessionId())) {
            throw new IllegalArgumentException("Session ID is required");
        }

        if (!StringUtils.hasText(command.getLanguage())) {
            // Default to English if not specified
            command.setLanguage("en");
        }

        log.debug("Validated analyze intent command: {}", command);
    }

    /**
     * Validate an extract parameters command.
     *
     * @param command The command to validate
     * @throws IllegalArgumentException if the command is invalid
     */
    public void validateExtractParameters(ExtractParametersCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        if (!StringUtils.hasText(command.getAnalysisId())) {
            throw new IllegalArgumentException("Analysis ID is required");
        }

        if (!StringUtils.hasText(command.getUserInput())) {
            throw new IllegalArgumentException("User input is required");
        }

        if (command.getIntents() == null || command.getIntents().isEmpty()) {
            throw new IllegalArgumentException("At least one intent is required");
        }

        if (!StringUtils.hasText(command.getLanguage())) {
            // Default to English if not specified
            command.setLanguage("en");
        }

        log.debug("Validated extract parameters command: {}", command);
    }
}