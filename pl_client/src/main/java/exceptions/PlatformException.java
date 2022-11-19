package exceptions;

public class PlatformException extends InternalException {

    private static final String topic = "Platform exception: ";

    public PlatformException(String errorMessage, Exception exception) {
        super(topic + errorMessage, exception);
    }

}