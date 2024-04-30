package com.igot.demand.exception;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
@Builder
@Getter
@Setter
public class ErrorResponse {
    private String code;
    private String message;
    private int httpStatusCode;
}
