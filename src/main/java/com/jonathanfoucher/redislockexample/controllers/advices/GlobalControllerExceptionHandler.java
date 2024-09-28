package com.jonathanfoucher.redislockexample.controllers.advices;

import com.jonathanfoucher.redislockexample.errors.JobAlreadyProcessedException;
import com.jonathanfoucher.redislockexample.errors.JobLockedException;
import com.jonathanfoucher.redislockexample.errors.JobNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@ControllerAdvice
public class GlobalControllerExceptionHandler {
    @ExceptionHandler(JobNotFoundException.class)
    public final ResponseEntity<String> handleNotFoundException(Exception exception) {
        log.warn(exception.getMessage());
        return ResponseEntity.status(NOT_FOUND)
                .body(exception.getMessage());
    }

    @ExceptionHandler(JobLockedException.class)
    public final ResponseEntity<String> handleLockedException(Exception exception) {
        log.warn(exception.getMessage());
        return ResponseEntity.status(LOCKED)
                .body(exception.getMessage());
    }

    @ExceptionHandler(JobAlreadyProcessedException.class)
    public final ResponseEntity<String> handleBadRequestException(Exception exception) {
        log.warn(exception.getMessage());
        return ResponseEntity.status(BAD_REQUEST)
                .body(exception.getMessage());
    }
}
