package io.watermelon.ci.common.error;

public class PlatformException extends RuntimeException {
    private final ErrorCode code;

    public PlatformException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public PlatformException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
