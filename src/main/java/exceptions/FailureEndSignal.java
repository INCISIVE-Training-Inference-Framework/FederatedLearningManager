package exceptions;

public class FailureEndSignal extends Exception {

    public FailureEndSignal(String errorMessage) {
        super(errorMessage);
    }

}