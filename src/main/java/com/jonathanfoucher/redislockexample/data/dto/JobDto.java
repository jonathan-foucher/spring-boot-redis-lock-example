package com.jonathanfoucher.redislockexample.data.dto;

import com.jonathanfoucher.redislockexample.data.enums.JobStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class JobDto {
    private Long id;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private JobStatus status;

    @Override
    public String toString() {
        return String.format("{ id=%s, name=\"%s\", start_date=%s, end_date=%s, status=%s}", id, name, startDate, endDate, status);
    }
}
