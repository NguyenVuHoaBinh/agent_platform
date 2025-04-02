package viettel.dac.intentanalysisservice.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import viettel.dac.intentanalysisservice.config.LLMProperties;
import viettel.dac.intentanalysisservice.exception.LLMException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LLMClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LLMProperties llmProperties;

    @Mock
    private LLMFallbackHandler fallbackHandler;

    @InjectMocks
    private ResilienceLLMClient llmClient;

    @Captor
    private ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor;

    private String testPrompt;
    private Map<String, Object> mockResponse;

    @BeforeEach
    void setUp() {
        testPrompt = "Test prompt";

        when(llmProperties.getApiUrl()).thenReturn("https://api.test.com/v1/completions");
        when(llmProperties.getModel()).thenReturn("test-model");
        when(llmProperties.getSystemPrompt()).thenReturn("You are an assistant");
        when(llmProperties.getTemperature()).thenReturn(0.3);
        when(llmProperties.getMaxTokens()).thenReturn(1024);

        // Setup mock response
        Map<String, Object> message = new HashMap<>();
        message.put("content", "Test response");

        Map<String, Object> choice = new HashMap<>();
        choice.put("message", message);

        mockResponse = new HashMap<>();
        mockResponse.put("choices", List.of(choice));
    }

    @Test
    void getCompletion_ShouldReturnLLMResponse() {
        // Arrange
        when(restTemplate.postForObject(
                eq(llmProperties.getApiUrl()),
                any(),
                eq(Map.class)
        )).thenReturn(mockResponse);

        // Act
        String result = llmClient.getCompletion(testPrompt);

        // Assert
        assertEquals("Test response", result);
        verify(restTemplate, times(1)).postForObject(
                eq(llmProperties.getApiUrl()),
                any(),
                eq(Map.class)
        );
    }

    @Test
    void getCompletion_WhenAPIFails_ShouldThrowException() {
        // Arrange
        when(restTemplate.postForObject(
                eq(llmProperties.getApiUrl()),
                any(),
                eq(Map.class)
        )).thenThrow(new RuntimeException("API error"));

        // Act & Assert
        assertThrows(LLMException.class, () -> llmClient.getCompletion(testPrompt));
    }

    @Test
    void getCompletionAsync_ShouldReturnCompletableFuture() {
        // Arrange
        when(restTemplate.postForObject(
                eq(llmProperties.getApiUrl()),
                any(),
                eq(Map.class)
        )).thenReturn(mockResponse);

        // Act
        var future = llmClient.getCompletionAsync(testPrompt);

        // Assert
        assertNotNull(future);
        assertDoesNotThrow(() -> future.join());
        assertEquals("Test response", future.join());
    }
}