package exceptions;

public class CommunicationException extends InternalException {

    private static final String topic = "Communication exception: ";

    public CommunicationException(String errorMessage, Exception exception) {
        super(topic + errorMessage, exception);
    }

}