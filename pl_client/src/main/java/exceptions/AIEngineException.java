package exceptions;

public class AIEngineException extends InternalException {

    private static final String topic = "AI Engine exception: ";

    public AIEngineException(String errorMessage, Exception exception) {
        super(topic + errorMessage, exception);
    }

}