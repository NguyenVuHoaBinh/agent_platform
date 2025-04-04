package viettel.dac.toolserviceregistry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.toolserviceregistry.exception.InvalidParameterTypeException;
import viettel.dac.toolserviceregistry.exception.InvalidValidationPatternException;
import viettel.dac.toolserviceregistry.repository.ToolRepository;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Validator for tool-related validation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ToolValidator {
    private final ToolRepository toolRepository;

    /**
     * Validates a parameter type.
     *
     * @param name The parameter name
     * @param type The parameter type
     */
    public void validateParameterType(String name, String type) {
        if (type == null || type.isEmpty()) {
            throw new InvalidParameterTypeException(name, "Type cannot be empty");
        }

        // Check that type is one of the allowed types
        if (!Arrays.asList("string", "number", "boolean", "array", "object").contains(type.toLowerCase())) {
            throw new InvalidParameterTypeException(name,
                    "Type must be one of: string, number, boolean, array, object");
        }
    }

    /**
     * Validates a validation pattern.
     *
     * @param name The parameter name
     * @param pattern The validation pattern
     */
    public void validateValidationPattern(String name, String pattern) {
        if (pattern != null && !pattern.isEmpty()) {
            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                throw new InvalidValidationPatternException(name, e.getMessage());
            }
        }
    }
}
