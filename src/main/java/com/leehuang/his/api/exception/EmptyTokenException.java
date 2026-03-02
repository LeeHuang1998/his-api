package com.leehuang.his.api.exception;

public class EmptyTokenException extends RuntimeException {
    public EmptyTokenException(String message) {
        super(message);
    }
}
