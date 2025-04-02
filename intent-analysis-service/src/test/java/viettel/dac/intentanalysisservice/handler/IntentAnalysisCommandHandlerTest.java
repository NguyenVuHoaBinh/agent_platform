package viettel.dac.intentanalysisservice.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import viettel.dac.intentanalysisservice.dto.ToolDTO;
import viettel.dac.intentanalysisservice.dto.ToolParameterDTO;
import viettel.dac.intentanalysisservice.event.EventPublisher;
import viettel.dac.intentanalysisservice.event.IntentAnalysisCompletedEvent;
import viettel.dac.intentanalysisservice.llm.LLMClient;
import viettel.dac.intentanalysisservice.model.AnalyzeIntentCommand;
import viettel.dac.intentanalysisservice.model.ExtractParametersCommand;
import viettel.dac.intentanalysisservice.model.Intent;
import viettel.dac.intentanalysisservice.model.IntentWithParameters;
import viettel.dac.intentanalysisservice.service.PromptTemplateService;
import viettel.dac.intentanalysisservice.service.ToolService;
import viettel.dac.intentanalysisservice.util.JsonUtil;
import viettel.dac.intentanalysisservice.validator.IntentCommandValidator;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IntentAnalysisCommandHandlerTest {

    @Mock
    private LLMClient llmClient;

    @Mock
    private PromptTemplateService promptService;

    @Mock
    private ToolService toolService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IntentCommandValidator validator;

    @Mock
    private JsonUtil jsonUtil;

    @InjectMocks
    private IntentAnalysisCommandHandler commandHandler;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private AnalyzeIntentCommand analyzeCommand;
    private ExtractParametersCommand extractCommand;
    private List<ToolDTO> tools;
    private String llmIntentResponse;
    private String llmParameterResponse;
    private List<Intent> intents;
    private List<IntentWithParameters> intentsWithParameters;

    @BeforeEach
    void setUp() {
        // Setup test data
        analyzeCommand = AnalyzeIntentCommand.builder()
                .userInput("I want to search for toothbrushes")
                .sessionId("test-session")
                .build();

        // Create tools
        ToolParameterDTO parameterDTO = new ToolParameterDTO();
        parameterDTO.setName("keyword");
        parameterDTO.setDescription("Search keyword");
        parameterDTO.setParameterType("string");
        parameterDTO.setRequired(true);

        ToolDTO toolDTO = new ToolDTO();
        toolDTO.setId("tool1");
        toolDTO.setName("search_product");
        toolDTO.setDescription("Search for products");
        toolDTO.setParameters(List.of(parameterDTO));
        toolDTO.setActive(true);

        tools = List.of(toolDTO);

        // Create intents
        intents = List.of(new Intent("search_product", 0.95));

        // Create intents with parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("keyword", "toothbrushes");

        IntentWithParameters intentWithParams = new IntentWithParameters();
        intentWithParams.setIntent("search_product");
        intentWithParams.setParameters(parameters);
        intentWithParams.setConfidence(0.95);
        intentWithParams.setState(0);

        intentsWithParameters = List.of(intentWithParams);

        // LLM responses
        llmIntentResponse = "[{\"intent\": \"search_product\", \"confidence\": 0.95}]";
        llmParameterResponse = "[{\"intent\": \"search_product\", \"parameters\": {\"keyword\": \"toothbrushes\"}, \"confidence\": 0.95, \"state\": 0}]";

        // Create extract parameters command
        extractCommand = ExtractParametersCommand.builder()
                .analysisId("test-analysis-id")
                .userInput("I want to search for toothbrushes")
                .intents(intents)
                .build();

        // Configure mocks for successful path
        when(toolService.getTools(any())).thenReturn(tools);
        when(promptService.createIntentAnalysisPrompt(any(), any())).thenReturn("test prompt");
        when(promptService.createParameterExtractionPrompt(any(), any(), any())).thenReturn("test param prompt");
        when(llmClient.getCompletion("test prompt")).thenReturn(llmIntentResponse);
        when(llmClient.getCompletion("test param prompt")).thenReturn(llmParameterResponse);
        when(jsonUtil.extractJsonArray(llmIntentResponse)).thenReturn(llmIntentResponse);
        when(jsonUtil.extractJsonArray(llmParameterResponse)).thenReturn(llmParameterResponse);
        when(jsonUtil.fromJsonList(llmIntentResponse, Intent.class)).thenReturn(intents);
        when(jsonUtil.fromJsonList(llmParameterResponse, IntentWithParameters.class)).thenReturn(intentsWithParameters);
        when(toolService.getToolsByNames(any())).thenReturn(tools);
    }

    @Test
    void handleAnalyzeIntent_ShouldReturnAnalysisIdAndPublishEvents() {
        // Act
        String analysisId = commandHandler.handleAnalyzeIntent(analyzeCommand);

        // Assert
        assertNotNull(analysisId);

        // Verify interactions
        verify(validator).validateAnalyzeIntent(analyzeCommand);
        verify(toolService).getTools(null);
        verify(promptService).createIntentAnalysisPrompt(analyzeCommand.getUserInput(), tools);
        verify(llmClient).getCompletion("test prompt");
        verify(jsonUtil).extractJsonArray(llmIntentResponse);
        verify(jsonUtil).fromJsonList(llmIntentResponse, Intent.class);

        // Verify events - use times(2) to expect two publish calls
        verify(eventPublisher, times(2)).publish(eq("intent-analysis-events"), eventCaptor.capture());

        // Get the last captured event (which should be the completed event)
        List<Object> capturedEvents = eventCaptor.getAllValues();
        Object lastEvent = capturedEvents.get(capturedEvents.size() - 1);

        assertTrue(lastEvent instanceof IntentAnalysisCompletedEvent);
        IntentAnalysisCompletedEvent completedEvent = (IntentAnalysisCompletedEvent) lastEvent;
        assertEquals(analysisId, completedEvent.getAnalysisId());
        assertEquals(analyzeCommand.getUserInput(), completedEvent.getUserInput());
        assertEquals(analyzeCommand.getSessionId(), completedEvent.getSessionId());
        assertEquals(intents, completedEvent.getIntents());
        assertTrue(completedEvent.getConfidence() > 0);
    }


    @Test
    void handleExtractParameters_ShouldReturnIntentsWithParametersAndPublishEvent() {
        // Act
        List<IntentWithParameters> result = commandHandler.handleExtractParameters(extractCommand);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("search_product", result.get(0).getIntent());
        assertEquals(0.95, result.get(0).getConfidence());
        assertEquals("toothbrushes", result.get(0).getParameters().get("keyword"));

        // Verify interactions
        verify(validator).validateExtractParameters(extractCommand);
        verify(toolService).getToolsByNames(List.of("search_product"));
        verify(promptService).createParameterExtractionPrompt(
                extractCommand.getUserInput(), extractCommand.getIntents(), tools);
        verify(llmClient).getCompletion("test param prompt");
        verify(jsonUtil).extractJsonArray(llmParameterResponse);
        verify(jsonUtil).fromJsonList(llmParameterResponse, IntentWithParameters.class);

        // Verify events
        verify(eventPublisher).publish(eq("intent-analysis-events"), any());
    }
}
