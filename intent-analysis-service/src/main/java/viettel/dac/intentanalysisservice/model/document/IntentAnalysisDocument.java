package viettel.dac.intentanalysisservice.model.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch document for storing intent analysis data.
 */
@Document(indexName = "intent_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentAnalysisDocument {

    /**
     * Unique identifier for the analysis, matches the analysisId used in events.
     */
    @Id
    private String analysisId;

    /**
     * The original user input text that was analyzed.
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String userInput;

    /**
     * User's session identifier for tracking conversation context.
     */
    @Field(type = FieldType.Keyword)
    private String sessionId;

    /**
     * List of intents identified in the analysis.
     */
    @Field(type = FieldType.Nested)
    private List<IntentDocument> intents;

    /**
     * Flag indicating if multiple intents were identified.
     */
    @Field(type = FieldType.Boolean)
    private boolean multiIntent;

    /**
     * Optional identifier for the conversation flow.
     */
    @Field(type = FieldType.Keyword)
    private String flowId;

    /**
     * Overall confidence score for the analysis.
     */
    @Field(type = FieldType.Double)
    private double confidence;

    /**
     * Final text response generated (if applicable).
     */
    @Field(type = FieldType.Text)
    private String finalText;

    /**
     * Status code (0=pending, 1=active, 2=done, 3=failed).
     */
    @Field(type = FieldType.Integer)
    private int status;

    /**
     * Time taken to process in milliseconds.
     */
    @Field(type = FieldType.Long)
    private long processingTimeMs;

    /**
     * Timestamp when the analysis was performed.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    /**
     * Optional metadata associated with the analysis.
     */
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;
}