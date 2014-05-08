package nl.gmt.rollbase.support;

public class RbWalkerException extends RuntimeException {
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
