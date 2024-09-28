package com.jonathanfoucher.redislockexample.controllers;

import com.jonathanfoucher.redislockexample.data.dto.JobDto;
import com.jonathanfoucher.redislockexample.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/jobs")
public class JobController {
    private final JobService jobService;

    @GetMapping
    public List<JobDto> getAllJobs() {
        return jobService.getAllJobs();
    }

    @PostMapping
    public Long createJob(@RequestParam String name) {
        return jobService.createJob(name);
    }

    @PostMapping("/{id}/start")
    public void startJob(@PathVariable Long id) {
        jobService.startJob(id);
    }
}
