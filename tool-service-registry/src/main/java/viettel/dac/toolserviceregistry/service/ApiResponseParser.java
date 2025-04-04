package viettel.dac.toolserviceregistry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import viettel.dac.toolserviceregistry.exception.ApiResponseParsingException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiResponseParser {
    private final ObjectMapper objectMapper;

    /**
     * Extracts a value from a response using the extraction path.
     *
     * @param response The response string
     * @param extractionPath The path to extract (JSONPath, XPath, or regex)
     * @param format The format of the response (json, xml, text)
     * @return The extracted value
     */
    public Object extractValue(String response, String extractionPath, String format) {
        if (response == null || extractionPath == null || extractionPath.isEmpty()) {
            return null;
        }

        try {
            switch (format.toLowerCase()) {
                case "json":
                    return extractJsonValue(response, extractionPath);
                case "xml":
                    return extractXmlValue(response, extractionPath);
                case "text":
                    return extractTextValue(response, extractionPath);
                default:
                    log.warn("Unsupported response format: {}", format);
                    return null;
            }
        } catch (Exception e) {
            log.error("Error extracting value from response using path: {}", extractionPath, e);
            throw new ApiResponseParsingException("Failed to extract value: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts a value from a JSON response using JSONPath.
     */
    private Object extractJsonValue(String response, String jsonPath) {
        try {
            return JsonPath.read(response, jsonPath);
        } catch (PathNotFoundException e) {
            log.warn("JSONPath not found: {}", jsonPath);
            return null;
        }
    }

    /**
     * Extracts a value from an XML response using XPath.
     */
    private Object extractXmlValue(String response, String xpath) throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));

            XPath xPath = XPathFactory.newInstance().newXPath();
            return xPath.evaluate(xpath, document);
        } catch (Exception e) {
            log.error("Error parsing XML or evaluating XPath: {}", xpath, e);
            throw e;
        }
    }

    /**
     * Extracts a value from a text response using regex.
     */
    private Object extractTextValue(String response, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            return matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
        }

        return null;
    }

    /**
     * Determines the format of a response.
     *
     * @param response The response string
     * @return The detected format (json, xml, text)
     */
    public String detectResponseFormat(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "text";
        }

        // Try to parse as JSON
        try {
            objectMapper.readTree(response);
            return "json";
        } catch (JsonProcessingException e) {
            // Not JSON, try XML
        }

        // Try to parse as XML
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
            return "xml";
        } catch (Exception e) {
            // Not XML, treat as text
        }

        return "text";
    }
}