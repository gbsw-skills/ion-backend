package com.ion.common.exception;

public class IonException extends RuntimeException {

    private final ErrorCode errorCode;

    public IonException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
