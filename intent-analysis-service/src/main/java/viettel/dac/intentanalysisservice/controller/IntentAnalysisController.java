package viettel.dac.intentanalysisservice.controller;

import viettel.dac.intentanalysisservice.exception.ErrorResponse;
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
public class IntentAnalysisController {

    private final IntentAnalysisCommandService commandService;

    /**
     * Analyze user input for intents (synchronous).
     *
     * @param request The analysis request
     * @return The analysis result
     */
    @PostMapping
    public ResponseEntity<IntentAnalysisResponse> analyzeIntent(
            @Valid @RequestBody IntentAnalysisRequest request) {

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
    public ResponseEntity<AsyncAnalysisResponse> analyzeIntentAsync(
            @Valid @RequestBody IntentAnalysisRequest request) {

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
    public ResponseEntity<?> getAnalysisStatus(@PathVariable String analysisId) {
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
    public static class IntentAnalysisRequest {
        private String userInput;
        private String sessionId;
        private java.util.List<String> toolIds;
        private String language;
        private java.util.Map<String, Object> metadata;

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