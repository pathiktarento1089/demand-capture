package com.igot.demand.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DemandCustomException extends RuntimeException {
    private String code;
    private String message;
    private int httpStatusCode;

    public DemandCustomException(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public DemandCustomException(String code, String message, int httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    public DemandCustomException() {

    }
}

