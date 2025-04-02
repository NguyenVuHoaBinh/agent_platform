package viettel.dac.intentanalysisservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import viettel.dac.intentanalysisservice.dto.AnalysisStatusResponse;
import viettel.dac.intentanalysisservice.dto.AsyncAnalysisResponse;
import viettel.dac.intentanalysisservice.dto.IntentAnalysisResponse;
import viettel.dac.intentanalysisservice.exception.ErrorResponse;
import viettel.dac.intentanalysisservice.exception.NotFoundException;
import viettel.dac.intentanalysisservice.service.IntentAnalysisCommandService;
import viettel.dac.intentanalysisservice.service.IntentAnalysisCommandService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.intentanalysisservice.model.AnalyzeIntentCommand;

import java.time.LocalDateTime;

/**
 * REST controller for intent analysis.
 */
@RestController
@RequestMapping("/analyze")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Intent Analysis", description = "APIs for analyzing user intents")
public class IntentAnalysisController {

    private final IntentAnalysisCommandService commandService;

    /**
     * Analyze user input for intents (synchronous).
     *
     * @param request The analysis request
     * @return The analysis result
     */
    @PostMapping
    @Operation(
            summary = "Analyze user input for intents",
            description = "Analyzes the provided user input text to identify intents and extract parameters",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful analysis",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = IntentAnalysisResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<IntentAnalysisResponse> analyzeIntent(
            @Valid @RequestBody
            @Parameter(description = "Analysis request containing user input and context", required = true)
            IntentAnalysisRequest request) {

        log.info("Received intent analysis request for user input: '{}'", request.getUserInput());

        // Convert request to command
        AnalyzeIntentCommand command = AnalyzeIntentCommand.builder()
                .userInput(request.getUserInput())
                .sessionId(request.getSessionId())
                .toolIds(request.getToolIds())
                .language(request.getLanguage())
                .metadata(request.getMetadata())
                .build();

        // Execute command
        IntentAnalysisResponse response = commandService.analyzeIntent(command);

        return ResponseEntity.ok(response);
    }

    /**
     * Analyze user input for intents asynchronously.
     *
     * @param request The analysis request
     * @return Response with analysis ID for checking status
     */
    @PostMapping("/async")
    @Operation(
            summary = "Analyze user input asynchronously",
            description = "Starts asynchronous analysis of user input and returns an ID to check the status later",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Analysis started",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AsyncAnalysisResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<AsyncAnalysisResponse> analyzeIntentAsync(
            @Valid @RequestBody
            @Parameter(description = "Analysis request containing user input and context", required = true)
            IntentAnalysisRequest request) {

        log.info("Received async intent analysis request for user input: '{}'", request.getUserInput());

        // Convert request to command
        AnalyzeIntentCommand command = AnalyzeIntentCommand.builder()
                .userInput(request.getUserInput())
                .sessionId(request.getSessionId())
                .toolIds(request.getToolIds())
                .language(request.getLanguage())
                .metadata(request.getMetadata())
                .build();

        // Start async analysis
        AsyncAnalysisResponse response = commandService.analyzeIntentAsync(command);

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Check the status of an asynchronous analysis.
     *
     * @param analysisId The analysis ID
     * @return The current status
     */
    @GetMapping("/status/{analysisId}")
    @Operation(
            summary = "Get analysis status",
            description = "Retrieves the current status of an asynchronous analysis",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status retrieved",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AnalysisStatusResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Analysis not found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<?> getAnalysisStatus(
            @PathVariable
            @Parameter(description = "The ID of the analysis to check", required = true)
            String analysisId) {

        try {
            AnalysisStatusResponse status = commandService.getAnalysisStatus(analysisId);
            return ResponseEntity.ok(status);
        } catch (NotFoundException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .code("NOT_FOUND")
                    .message("Analysis not found")
                    .details(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Request object for intent analysis.
     */
    @Schema(description = "Request for intent analysis")
    public static class IntentAnalysisRequest {
        @Schema(description = "The user's input text to analyze", example = "I want to search for laptops", required = true)
        private String userInput;

        @Schema(description = "User's session identifier for tracking conversation context", example = "session-123", required = true)
        private String sessionId;

        @Schema(description = "Optional list of specific tool IDs to consider in the analysis")
        private java.util.List<String> toolIds;

        @Schema(description = "Language code (defaults to 'en' for English)", example = "en", defaultValue = "en")
        private String language;

        @Schema(description = "Optional metadata for additional context")
        private java.util.Map<String, Object> metadata;

        // Getters and setters as before
        public String getUserInput() {
            return userInput;
        }

        public void setUserInput(String userInput) {
            this.userInput = userInput;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public java.util.List<String> getToolIds() {
            return toolIds;
        }

        public void setToolIds(java.util.List<String> toolIds) {
            this.toolIds = toolIds;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public java.util.Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(java.util.Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}