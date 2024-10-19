package com.jonathanfoucher.redislockexample.services;

import com.jonathanfoucher.redislockexample.data.dto.JobDto;
import com.jonathanfoucher.redislockexample.data.model.Job;
import com.jonathanfoucher.redislockexample.data.repository.JobRepository;
import com.jonathanfoucher.redislockexample.errors.JobAlreadyProcessedException;
import com.jonathanfoucher.redislockexample.errors.JobLockedException;
import com.jonathanfoucher.redislockexample.errors.JobNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static com.jonathanfoucher.redislockexample.data.enums.JobStatus.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class JobService {
    private final JobRepository jobRepository;
    private final ExpirableLockRegistry redisLockRegistry;

    private static final int TRY_LOCK_TIMEOUT = 60;

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
        if (!WAITING.equals(job.get().getStatus())) {
            throw new JobAlreadyProcessedException(id);
        }
        processJob(job.get());
    }

    private void processJob(Job job) {
        Lock lock = getLock(job.getId());

        try {
            log.info("starting to process job {}", job.getId());
            job.setStartDate(LocalDateTime.now());
            // simulate running job
            doSomething();
            job.setStatus(SUCCESS);
            log.info("successfully processed job {}", job.getId());
        } catch (Exception e) {
            job.setStatus(ERROR);
            log.error(e.getMessage());
            log.error("failed to process job {}", job.getId());
        } finally {
            job.setEndDate(LocalDateTime.now());
            jobRepository.save(job);
            lock.unlock();
        }
    }

    private Lock getLock(Long jobId) {
        Lock lock = redisLockRegistry.obtain(String.valueOf(jobId));
        try {
            if (!lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.SECONDS)) {
                throw new JobLockedException(jobId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return lock;
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
        entity.setStatus(WAITING);
        return entity;
    }

    void doSomething() {
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
