package com.igot.demand.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandling {

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception ex) {
        log.debug("RestExceptionHandler::handleException::" + ex);
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        ErrorResponse errorResponse = null;
        if (ex instanceof DemandCustomException) {
            DemandCustomException demandCustomException = (DemandCustomException) ex;
            status = HttpStatus.BAD_REQUEST.value();
            if (demandCustomException.getHttpStatusCode() > 0) {
                try {
                    status = demandCustomException.getHttpStatusCode();
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid HTTP status code provided in DemandCustomException: " + demandCustomException.getHttpStatusCode());
                }
            }
            errorResponse = ErrorResponse.builder()
                    .code(demandCustomException.getCode())
                    .message(demandCustomException.getMessage())
                    .httpStatusCode(demandCustomException.getHttpStatusCode() > 0
                            ? demandCustomException.getHttpStatusCode()
                            : status)
                    .build();
            if (StringUtils.isNotBlank(demandCustomException.getMessage())) {
                log.error(demandCustomException.getMessage());
            }

            return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(status));
        }
        errorResponse = ErrorResponse.builder()
                .code(ex.getMessage()).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(status));
    }

}
