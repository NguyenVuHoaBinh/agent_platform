package viettel.dac.intentanalysisservice.exception;

/**
 * Exception thrown when there's an issue with LLM service communication.
 */
public class LLMException extends RuntimeException {

    /**
     * Creates a new LLM exception with a message.
     *
     * @param message The exception message
     */
    public LLMException(String message) {
        super(message);
    }

    /**
     * Creates a new LLM exception with a message and cause.
     *
     * @param message The exception message
     * @param cause The underlying cause of the exception
     */
    public LLMException(String message, Throwable cause) {
        super(message, cause);
    }
}
