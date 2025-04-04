package viettel.dac.toolserviceregistry.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import viettel.dac.toolserviceregistry.model.reponse.ErrorResponse;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles validation exceptions.
     *
     * @param ex The exception
     * @return Error response with validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed")
                .details(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles tool not found exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(ToolNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleToolNotFoundException(ToolNotFoundException ex) {
        log.error("Tool not found: {}", ex.getMessage());

        return ErrorResponse.builder()
                .code("TOOL_NOT_FOUND")
                .message("Tool not found")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles duplicate tool name exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(DuplicateToolNameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateToolNameException(DuplicateToolNameException ex) {
        log.error("Duplicate tool name: {}", ex.getMessage());

        return ErrorResponse.builder()
                .code("DUPLICATE_TOOL_NAME")
                .message("Tool with this name already exists")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles cyclic dependency exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(CyclicDependencyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCyclicDependencyException(CyclicDependencyException ex) {
        log.error("Cyclic dependency detected: {}", ex.getMessage());

        return ErrorResponse.builder()
                .code("CYCLIC_DEPENDENCY")
                .message("Cyclic dependency detected")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles tool has dependents exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(ToolHasDependentsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleToolHasDependentsException(ToolHasDependentsException ex) {
        log.error("Tool has dependents: {}", ex.getMessage());

        return ErrorResponse.builder()
                .code("TOOL_HAS_DEPENDENTS")
                .message("Tool cannot be deleted because other tools depend on it")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles all other exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles tool type not compatible exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(ToolTypeNotCompatibleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleToolTypeNotCompatibleException(ToolTypeNotCompatibleException ex) {
        log.error("Tool type not compatible: {}", ex.getMessage());

        return ErrorResponse.builder()
                .code("TOOL_TYPE_NOT_COMPATIBLE")
                .message("Tool type is not compatible with the requested operation")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles API metadata not found exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(ApiMetadataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleApiMetadataNotFoundException(ApiMetadataNotFoundException ex) {
        log.error("API metadata not found: {}", ex.getMessage());

        return ErrorResponse.builder()
                .code("API_METADATA_NOT_FOUND")
                .message("API metadata not found")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles tool parameter not found exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(ToolParameterNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleToolParameterNotFoundException(ToolParameterNotFoundException ex) {
        log.error("Tool parameter not found: {}", ex.getMessage());

        return ErrorResponse.builder()
                .code("TOOL_PARAMETER_NOT_FOUND")
                .message("Tool parameter not found")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles resource not found exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());

        return ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .message("Resource not found")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Handles invalid parameter exceptions.
     *
     * @param ex The exception
     * @return Error response
     */
    @ExceptionHandler(InvalidParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidParameterException(InvalidParameterException ex) {
        log.error("Invalid parameter: {}", ex.getMessage());

        return ErrorResponse.builder()
                .code("INVALID_PARAMETER")
                .message("Invalid parameter value")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }
}