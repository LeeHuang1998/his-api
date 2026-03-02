package com.leehuang.his.api.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FileException extends RuntimeException{
    private final int code;

    public FileException(int code, String message) {
        super(message);
        this.code = code;
    }

    public FileException(int code, String message, Throwable e) {
        super(message, e);
        this.code = code;
    }
}
