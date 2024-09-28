package com.jonathanfoucher.redislockexample.errors;

public class JobLockedException extends RuntimeException {
    public JobLockedException(Long id) {
        super("Job locked for id=" + id);
    }
}
