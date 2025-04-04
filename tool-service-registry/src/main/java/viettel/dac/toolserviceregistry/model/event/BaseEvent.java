package viettel.dac.toolserviceregistry.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all events in the system.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the event
     */
    private String eventId;

    /**
     * Type of the event
     */
    private String eventType;

    /**
     * Timestamp when the event was created
     */
    private LocalDateTime timestamp;

    /**
     * Additional metadata for the event
     */
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Initializes a new event with a random ID and current timestamp.
     *
     * @param eventType The type of the event
     */
    public BaseEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }

    /**
     * Adds a metadata item to the event.
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
}