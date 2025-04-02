package viettel.dac.intentanalysisservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import viettel.dac.intentanalysisservice.dto.IntentAnalysisResponse;
import viettel.dac.intentanalysisservice.exception.ErrorResponse;
import viettel.dac.intentanalysisservice.exception.NotFoundException;
import viettel.dac.intentanalysisservice.query.service.IntentAnalysisQueryService;
import viettel.dac.intentanalysisservice.query.service.IntentAnalysisQueryService.AnalysisHistoryResponse;

import java.time.LocalDateTime;

/**
 * REST controller for querying analysis history.
 */
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analysis History", description = "APIs for retrieving analysis history")
public class AnalysisHistoryController {

    private final IntentAnalysisQueryService queryService;

    /**
     * Get analysis history for a session.
     *
     * @param sessionId Session identifier
     * @param page Page number (0-based)
     * @param size Page size
     * @return History response
     */
    @GetMapping
    @Operation(
            summary = "Get analysis history for a session",
            description = "Retrieves the analysis history for a specific session with pagination",
            responses = {
                    @ApiResponse(responseCode = "200", description = "History retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AnalysisHistoryResponse.class)))
            }
    )
    public ResponseEntity<AnalysisHistoryResponse> getAnalysisHistory(
            @RequestParam
            @Parameter(description = "User's session identifier", required = true, example = "session-123")
            String sessionId,

            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number (0-based)", example = "0")
            int page,

            @RequestParam(defaultValue = "20")
            @Parameter(description = "Page size", example = "20")
            int size) {

        log.info("Received request for analysis history, sessionId: {}", sessionId);
        AnalysisHistoryResponse response = queryService.getAnalysisHistory(sessionId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get analysis details by ID.
     *
     * @param analysisId Analysis identifier
     * @return Analysis details
     */
    @GetMapping("/{analysisId}")
    @Operation(
            summary = "Get analysis details by ID",
            description = "Retrieves the detailed results of a specific analysis",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Analysis details retrieved",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = IntentAnalysisResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Analysis not found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<?> getAnalysisDetails(
            @PathVariable
            @Parameter(description = "The ID of the analysis to retrieve", required = true, example = "analysis-123")
            String analysisId) {

        log.info("Received request for analysis details, analysisId: {}", analysisId);

        try {
            IntentAnalysisResponse response = queryService.getAnalysisDetails(analysisId);
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .code("NOT_FOUND")
                    .message("Analysis not found")
                    .details(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    /**
     * Search for analyses by user input.
     *
     * @param query Search query
     * @param page Page number (0-based)
     * @param size Page size
     * @return Search results
     */
    @GetMapping("/search")
    @Operation(
            summary = "Search analyses by user input",
            description = "Searches for analyses that contain the specified search term in the user input",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search results retrieved",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AnalysisHistoryResponse.class)))
            }
    )
    public ResponseEntity<AnalysisHistoryResponse> searchAnalyses(
            @RequestParam
            @Parameter(description = "Search term to find in user inputs", required = true, example = "laptop")
            String query,

            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number (0-based)", example = "0")
            int page,

            @RequestParam(defaultValue = "20")
            @Parameter(description = "Page size", example = "20")
            int size) {

        log.info("Received search request, query: {}", query);
        AnalysisHistoryResponse response = queryService.searchAnalyses(query, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Find analyses by specific intent.
     *
     * @param intent Intent name
     * @param page Page number (0-based)
     * @param size Page size
     * @return Matching analyses
     */
    @GetMapping("/intent/{intent}")
    @Operation(
            summary = "Find analyses by intent",
            description = "Finds analyses that contain the specified intent",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Analyses retrieved",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AnalysisHistoryResponse.class)))
            }
    )
    public ResponseEntity<AnalysisHistoryResponse> findAnalysesByIntent(
            @PathVariable
            @Parameter(description = "Intent name to search for", required = true, example = "search_product")
            String intent,

            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number (0-based)", example = "0")
            int page,

            @RequestParam(defaultValue = "20")
            @Parameter(description = "Page size", example = "20")
            int size) {

        log.info("Received request for analyses by intent, intent: {}", intent);
        AnalysisHistoryResponse response = queryService.findAnalysesByIntent(intent, page, size);
        return ResponseEntity.ok(response);
    }
}