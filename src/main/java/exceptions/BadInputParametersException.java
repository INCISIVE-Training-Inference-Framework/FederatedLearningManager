package exceptions;

public class BadInputParametersException extends Exception {

    private static final String topic = "Bad input parameters exception: ";

    public BadInputParametersException(String errorMessage) {
        super(topic + errorMessage);
    }

}