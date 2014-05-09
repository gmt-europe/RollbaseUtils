package nl.gmt.rollbase.shared;

public class RbWalkerException extends Exception {
    public RbWalkerException() {
    }

    public RbWalkerException(String message) {
        super(message);
    }

    public RbWalkerException(String message, Throwable cause) {
        super(message, cause);
    }

    public RbWalkerException(Throwable cause) {
        super(cause);
    }

    public RbWalkerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
