package io.watermelon.ci.common.error;

public class NotFoundException extends PlatformException {
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
