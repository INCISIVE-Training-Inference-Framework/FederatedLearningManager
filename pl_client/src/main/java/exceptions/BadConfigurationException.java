package exceptions;

public class BadConfigurationException extends Exception {

    private static final String topic = "Bad configuration exception: ";

    public BadConfigurationException(String errorMessage) {
        super(topic + errorMessage);
    }
}