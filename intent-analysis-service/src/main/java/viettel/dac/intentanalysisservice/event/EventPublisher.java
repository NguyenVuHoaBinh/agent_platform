package viettel.dac.intentanalysisservice.event;

/**
 * Interface for publishing events to message brokers.
 */
public interface EventPublisher {

    /**
     * Publish an event to the specified topic.
     *
     * @param topic The topic to publish to
     * @param event The event to publish
     */
    void publish(String topic, Object event);
}
