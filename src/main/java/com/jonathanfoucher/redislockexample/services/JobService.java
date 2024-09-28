package com.jonathanfoucher.redislockexample.services;

import com.jonathanfoucher.redislockexample.data.dto.JobDto;
import com.jonathanfoucher.redislockexample.data.enums.JobStatus;
import com.jonathanfoucher.redislockexample.data.model.Job;
import com.jonathanfoucher.redislockexample.data.repository.JobRepository;
import com.jonathanfoucher.redislockexample.errors.JobNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class JobService {
    private final JobRepository jobRepository;

    public List<JobDto> getAllJobs() {
        return jobRepository.findAll()
                .stream()
                .map(this::convertEntityToDto)
                .toList();
    }

    public Long createJob(String name) {
        Job job = createJobEntity(name);
        return jobRepository.save(job)
                .getId();
    }

    public void startJob(Long id) {
        Optional<Job> job = jobRepository.findById(id);
        if (job.isEmpty()) {
            throw new JobNotFoundException(id);
        }

        processJob(job.get());
    }

    private void processJob(Job job) {
        log.info("starting to process job {}", job);
        job.setStatus(JobStatus.RUNNING);
        job.setStartDate(LocalDateTime.now());
        try {
            // simulate running job
            TimeUnit.SECONDS.sleep(10);
            log.info("successfully processed job {}", job);
            job.setStatus(JobStatus.SUCCESS);
        } catch (InterruptedException e) {
            log.error("failed to process job {}", job);
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
            job.setStatus(JobStatus.ERROR);
        } finally {
            job.setEndDate(LocalDateTime.now());
            jobRepository.save(job);
        }
    }

    private JobDto convertEntityToDto(Job entity) {
        JobDto dto = new JobDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private Job createJobEntity(String name) {
        Job entity = new Job();
        entity.setName(name);
        entity.setStatus(JobStatus.WAIT);
        return entity;
    }
}
