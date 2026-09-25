package com.enelrith.ordinator.common.exception;

public class NotAllowedException extends RuntimeException {
    public NotAllowedException(String message) {
        super(message);
    }

    public NotAllowedException() {}
}
