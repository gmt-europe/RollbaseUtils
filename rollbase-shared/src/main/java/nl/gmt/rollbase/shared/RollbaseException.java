package nl.gmt.rollbase.shared;

public class RollbaseException extends Exception {
    public RollbaseException() {
    }

    public RollbaseException(String message) {
        super(message);
    }

    public RollbaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public RollbaseException(Throwable cause) {
        super(cause);
    }

    public RollbaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
