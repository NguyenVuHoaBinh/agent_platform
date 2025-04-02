package viettel.dac.intentanalysisservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import viettel.dac.intentanalysisservice.dto.AsyncAnalysisResponse;
import viettel.dac.intentanalysisservice.dto.IntentAnalysisResponse;
import viettel.dac.intentanalysisservice.exception.NotFoundException;
import viettel.dac.intentanalysisservice.model.AnalyzeIntentCommand;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;
import viettel.dac.intentanalysisservice.service.IntentAnalysisCommandService;
import viettel.dac.intentanalysisservice.dto.AnalysisStatusResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IntentAnalysisController.class)
public class IntentAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntentAnalysisCommandService commandService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void analyzeIntent_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        IntentAnalysisController.IntentAnalysisRequest request = new IntentAnalysisController.IntentAnalysisRequest();
        request.setUserInput("search for toothbrushes");
        request.setSessionId("test-session");

        IntentAnalysisResponse mockResponse = new IntentAnalysisResponse();
        mockResponse.setCode("200");
        mockResponse.setAnalysisId("test-analysis-id");
        mockResponse.setMultiIntent(false);
        mockResponse.setConfidence(0.95);

        List<IntentWithParameters> intents = new ArrayList<>();
        IntentWithParameters intent = new IntentWithParameters();
        intent.setIntent("search_product");
        intent.setConfidence(0.95);
        intent.setState(0);
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "toothbrushes");
        intent.setParameters(params);
        intents.add(intent);
        mockResponse.setIntents(intents);

        when(commandService.analyzeIntent(any(AnalyzeIntentCommand.class))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.analysisId").value("test-analysis-id"))
                .andExpect(jsonPath("$.multiIntent").value(false))
                .andExpect(jsonPath("$.confidence").value(0.95))
                .andExpect(jsonPath("$.intents[0].intent").value("search_product"))
                .andExpect(jsonPath("$.intents[0].parameters.keyword").value("toothbrushes"));
    }

    @Test
    void analyzeIntent_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        IntentAnalysisController.IntentAnalysisRequest request = new IntentAnalysisController.IntentAnalysisRequest();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyzeIntentAsync_ValidRequest_ReturnsAccepted() throws Exception {
        // Arrange
        IntentAnalysisController.IntentAnalysisRequest request = new IntentAnalysisController.IntentAnalysisRequest();
        request.setUserInput("search for smartphones");
        request.setSessionId("test-session");

        AsyncAnalysisResponse mockResponse = new AsyncAnalysisResponse();
        mockResponse.setAnalysisId("test-async-id");
        mockResponse.setMessage("Analysis started, check status for results");

        when(commandService.analyzeIntentAsync(any(AnalyzeIntentCommand.class))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/analyze/async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.analysisId").value("test-async-id"))
                .andExpect(jsonPath("$.message").value("Analysis started, check status for results"));
    }

    @Test
    void getAnalysisStatus_ValidId_ReturnsStatus() throws Exception {
        // Arrange
        String analysisId = "test-analysis-id";

        AnalysisStatusResponse mockResponse = new AnalysisStatusResponse();
        mockResponse.setAnalysisId(analysisId);
        mockResponse.setStatus("COMPLETED");
        mockResponse.setProgress(100);

        when(commandService.getAnalysisStatus(analysisId)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/analyze/status/{analysisId}", analysisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(analysisId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value(100));
    }

    @Test
    void getAnalysisStatus_InvalidId_ReturnsNotFound() throws Exception {
        // Arrange
        String analysisId = "non-existent-id";

        when(commandService.getAnalysisStatus(analysisId)).thenThrow(new NotFoundException("Analysis not found"));

        // Act & Assert
        mockMvc.perform(get("/analyze/status/{analysisId}", analysisId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
