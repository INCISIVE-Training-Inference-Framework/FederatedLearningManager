package exceptions;

public class InternalException extends Exception {

    private static final String topic = "Internal exception: ";
    private final Exception exception;

    public InternalException(String errorMessage, Exception exception) {
        super(topic + errorMessage);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}