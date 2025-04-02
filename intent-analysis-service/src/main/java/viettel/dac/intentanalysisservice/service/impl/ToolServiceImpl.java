package viettel.dac.intentanalysisservice.service.impl;

import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import viettel.dac.intentanalysisservice.dto.ToolDTO;
import viettel.dac.intentanalysisservice.service.ToolService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of ToolService using WebClient to interact with Tool Registry Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ToolServiceImpl implements ToolService {

    private final WebClient webClient;

    @Value("${tool.registry.url:http://tool-registry-service:8081}")
    private String toolRegistryUrl;

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getToolsFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "#toolIds != null ? #toolIds.toString() : 'all'", unless = "#result.isEmpty()")
    public List<ToolDTO> getTools(List<String> toolIds) {
        log.debug("Fetching tools with IDs: {}", toolIds);

        if (toolIds == null || toolIds.isEmpty()) {
            return getAllTools();
        }

        List<ToolDTO> tools = new ArrayList<>();
        for (String toolId : toolIds) {
            ToolDTO tool = getToolById(toolId);
            if (tool != null) {
                tools.add(tool);
            }
        }

        return tools;
    }

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getToolsByNamesFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "'byNames:' + #toolNames.toString()", unless = "#result.isEmpty()")
    public List<ToolDTO> getToolsByNames(List<String> toolNames) {
        log.debug("Fetching tools with names: {}", toolNames);

        if (toolNames == null || toolNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<ToolDTO> tools = new ArrayList<>();
        for (String name : toolNames) {
            ToolDTO tool = getToolByName(name);
            if (tool != null) {
                tools.add(tool);
            }
        }

        return tools;
    }

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getAllToolsFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "'all'", unless = "#result.isEmpty()")
    public List<ToolDTO> getAllTools() {
        log.debug("Fetching all tools");

        try {
            return webClient.get()
                    .uri(toolRegistryUrl + "/api/v1/tools/query?active=true")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ToolDTO>>() {})
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("Error fetching all tools: {}", ex.getMessage());
                        return Mono.just(Collections.emptyList());
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error fetching all tools: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getToolByIdFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "'id:' + #toolId", unless = "#result == null")
    public ToolDTO getToolById(String toolId) {
        log.debug("Fetching tool with ID: {}", toolId);

        try {
            return webClient.get()
                    .uri(toolRegistryUrl + "/api/v1/tools/query/{id}", toolId)
                    .retrieve()
                    .bodyToMono(ToolDTO.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.empty();
                        }
                        log.error("Error fetching tool by ID {}: {}", toolId, ex.getMessage());
                        return Mono.error(ex);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error fetching tool by ID {}: {}", toolId, e.getMessage());
            return null;
        }
    }

    @Override
    @CircuitBreaker(name = "toolService", fallbackMethod = "getToolByNameFallback")
    @Retry(name = "toolService")
    @Cacheable(value = "toolCache", key = "'name:' + #name", unless = "#result == null")
    public ToolDTO getToolByName(String name) {
        log.debug("Fetching tool with name: {}", name);

        try {
            return webClient.get()
                    .uri(toolRegistryUrl + "/api/v1/tools/query/name/{name}", name)
                    .retrieve()
                    .bodyToMono(ToolDTO.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.empty();
                        }
                        log.error("Error fetching tool by name {}: {}", name, ex.getMessage());
                        return Mono.error(ex);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error fetching tool by name {}: {}", name, e.getMessage());
            return null;
        }
    }

    // Fallback methods

    public List<ToolDTO> getToolsFallback(List<String> toolIds, Exception e) {
        log.warn("Using fallback for getTools: {}", e.getMessage());
        return Collections.emptyList();
    }

    public List<ToolDTO> getToolsByNamesFallback(List<String> toolNames, Exception e) {
        log.warn("Using fallback for getToolsByNames: {}", e.getMessage());
        return Collections.emptyList();
    }

    public List<ToolDTO> getAllToolsFallback(Exception e) {
        log.warn("Using fallback for getAllTools: {}", e.getMessage());
        return Collections.emptyList();
    }

    public ToolDTO getToolByIdFallback(String toolId, Exception e) {
        log.warn("Using fallback for getToolById: {}", e.getMessage());
        return null;
    }

    public ToolDTO getToolByNameFallback(String name, Exception e) {
        log.warn("Using fallback for getToolByName: {}", e.getMessage());
        return null;
    }
}
