package viettel.dac.intentanalysisservice.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

/**
 * Nested Elasticsearch document for storing intent data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentDocument {

    /**
     * The name of the identified intent.
     */
    @Field(type = FieldType.Keyword)
    private String intent;

    /**
     * Confidence score for this intent (0.0 to 1.0).
     */
    @Field(type = FieldType.Double)
    private double confidence;

    /**
     * Map of parameter names to their extracted values.
     */
    @Field(type = FieldType.Object)
    private Map<String, Object> parameters;

    /**
     * State of the intent execution (0=not executed, 1=executed).
     */
    @Field(type = FieldType.Integer)
    private int state;
}