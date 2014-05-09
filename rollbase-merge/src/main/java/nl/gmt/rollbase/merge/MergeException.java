package nl.gmt.rollbase.merge;

public class MergeException extends Exception {
    public MergeException() {
    }

    public MergeException(String message) {
        super(message);
    }

    public MergeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MergeException(Throwable cause) {
        super(cause);
    }

    public MergeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
