package viettel.dac.intentanalysisservice.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Base class for all intent analysis events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class IntentAnalysisEvent {

    /**
     * Unique identifier for the event.
     */
    private String eventId;

    /**
     * Type of the event (used for routing).
     */
    private String eventType;

    /**
     * ID of the analysis this event is related to.
     */
    private String analysisId;

    /**
     * Timestamp when the event was created.
     */
    private LocalDateTime timestamp;

    /**
     * Optional metadata for additional context.
     */
    private Map<String, Object> metadata;
}
