package nl.gmt.rollbase.merge;

public class ArgumentsException extends Exception {
    public ArgumentsException() {
    }

    public ArgumentsException(String message) {
        super(message);
    }

    public ArgumentsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArgumentsException(Throwable cause) {
        super(cause);
    }

    public ArgumentsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
