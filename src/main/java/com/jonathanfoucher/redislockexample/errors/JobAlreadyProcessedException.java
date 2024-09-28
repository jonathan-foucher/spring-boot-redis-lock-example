package com.jonathanfoucher.redislockexample.errors;

public class JobAlreadyProcessedException extends RuntimeException {
    public JobAlreadyProcessedException(Long id) {
        super("Job already processed for id=" + id);
    }
}
