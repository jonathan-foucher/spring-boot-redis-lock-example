package com.jonathanfoucher.redislockexample.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jonathanfoucher.redislockexample.data.dto.JobDto;
import com.jonathanfoucher.redislockexample.data.enums.JobStatus;
import com.jonathanfoucher.redislockexample.data.model.Job;
import com.jonathanfoucher.redislockexample.data.repository.JobRepository;
import com.jonathanfoucher.redislockexample.errors.JobAlreadyProcessedException;
import com.jonathanfoucher.redislockexample.errors.JobLockedException;
import com.jonathanfoucher.redislockexample.errors.JobNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

import static com.jonathanfoucher.redislockexample.data.enums.JobStatus.*;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(JobService.class)
class JobServiceTest {
    @SpyBean
    @Autowired
    private JobService jobService;
    @MockBean
    private JobRepository jobRepository;
    @MockBean
    private ExpirableLockRegistry redisLockRegistry;

    private static final Long ID = 15L;
    private static final String NAME = "SOME_JOB";
    private static final LocalDateTime START_DATE = LocalDateTime.now().minusMinutes(20);
    private static final LocalDateTime END_DATE = LocalDateTime.now().minusMinutes(19);
    private static final JobStatus STATUS = JobStatus.SUCCESS;

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void getAllJobs() {
        // GIVEN
        Job job = initJob();

        when(jobRepository.findAll())
                .thenReturn(List.of(job));

        // WHEN
        List<JobDto> results = jobService.getAllJobs();

        // THEN
        verify(jobRepository, times(1)).findAll();

        assertNotNull(results);
        assertEquals(1, results.size());
        checkJobDto(results.getFirst());
    }

    @Test
    void getAllJobsWithEmptyResult() {
        // GIVEN
        when(jobRepository.findAll())
                .thenReturn(emptyList());

        // WHEN
        List<JobDto> results = jobService.getAllJobs();

        // THEN
        verify(jobRepository, times(1)).findAll();

        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    void createJob() {
        // GIVEN
        Job job = initJob();

        when(jobRepository.save(any()))
                .thenReturn(job);

        // WHEN
        Long result = jobService.createJob(NAME);

        // THEN
        ArgumentCaptor<Job> capturedJob = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, times(1)).save(capturedJob.capture());

        assertEquals(ID, result);

        Job savedJob = capturedJob.getValue();
        assertNotNull(savedJob);
        assertNull(savedJob.getId());
        assertEquals(NAME, savedJob.getName());
        assertNull(savedJob.getStartDate());
        assertNull(savedJob.getEndDate());
        assertEquals(WAITING, savedJob.getStatus());
    }

    @Test
    void startJob() {
        // GIVEN
        Job job = new Job();
        job.setId(ID);
        job.setName(NAME);
        job.setStatus(WAITING);

        Lock lock = mock(Lock.class);

        when(jobRepository.findById(ID))
                .thenReturn(Optional.of(job));
        when(redisLockRegistry.obtain(String.valueOf(ID)))
                .thenReturn(lock);
        when(lock.tryLock())
                .thenReturn(true);

        // WHEN
        jobService.startJob(ID);

        // THEN
        ArgumentCaptor<Job> capturedJob = ArgumentCaptor.forClass(Job.class);
        InOrder inOrder = inOrder(jobRepository, redisLockRegistry, lock);
        inOrder.verify(jobRepository, times(1)).findById(ID);
        inOrder.verify(redisLockRegistry, times(1)).obtain(String.valueOf(ID));
        inOrder.verify(lock, times(1)).tryLock();
        inOrder.verify(jobRepository, times(1)).save(capturedJob.capture());
        inOrder.verify(lock, times(1)).unlock();

        Job savedJob = capturedJob.getValue();
        assertNotNull(savedJob);
        assertEquals(ID, savedJob.getId());
        assertEquals(NAME, savedJob.getName());
        assertNotNull(savedJob.getStartDate());
        assertNotNull(savedJob.getEndDate());
        assertEquals(SUCCESS, savedJob.getStatus());
    }

    @Test
    void startJobWithProcessingError() throws InterruptedException {
        // GIVEN
        Job job = new Job();
        job.setId(ID);
        job.setName(NAME);
        job.setStatus(WAITING);

        Lock lock = mock(Lock.class);

        when(jobRepository.findById(ID))
                .thenReturn(Optional.of(job));
        when(redisLockRegistry.obtain(String.valueOf(ID)))
                .thenReturn(lock);
        when(lock.tryLock())
                .thenReturn(true);

        doThrow(InterruptedException.class)
                .when(jobService)
                .doSomething();

        // WHEN
        jobService.startJob(ID);

        // THEN
        ArgumentCaptor<Job> capturedJob = ArgumentCaptor.forClass(Job.class);
        InOrder inOrder = inOrder(jobRepository, redisLockRegistry, lock);
        inOrder.verify(jobRepository, times(1)).findById(ID);
        inOrder.verify(redisLockRegistry, times(1)).obtain(String.valueOf(ID));
        inOrder.verify(lock, times(1)).tryLock();
        inOrder.verify(jobRepository, times(1)).save(capturedJob.capture());
        inOrder.verify(lock, times(1)).unlock();

        Job savedJob = capturedJob.getValue();
        assertNotNull(savedJob);
        assertEquals(ID, savedJob.getId());
        assertEquals(NAME, savedJob.getName());
        assertNotNull(savedJob.getStartDate());
        assertNotNull(savedJob.getEndDate());
        assertEquals(ERROR, savedJob.getStatus());
    }

    @Test
    void startJobWithJobNotFound() {
        // GIVEN
        Lock lock = mock(Lock.class);

        when(jobRepository.findById(ID))
                .thenReturn(Optional.empty());

        // WHEN
        assertThatThrownBy(() -> jobService.startJob(ID))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessage("Job not found for id=" + ID);

        // THEN
        verify(jobRepository, times(1)).findById(ID);
        verify(redisLockRegistry, never()).obtain(any());
        verify(lock, never()).tryLock();
        verify(jobRepository, never()).save(any());
        verify(lock, never()).unlock();
    }

    @Test
    void startJobWithJobAlreadyProcessed() {
        // GIVEN
        Job job = new Job();
        job.setId(ID);
        job.setName(NAME);
        job.setStatus(SUCCESS);

        Lock lock = mock(Lock.class);

        when(jobRepository.findById(ID))
                .thenReturn(Optional.of(job));

        // WHEN
        assertThatThrownBy(() -> jobService.startJob(ID))
                .isInstanceOf(JobAlreadyProcessedException.class)
                .hasMessage("Job already processed for id=" + ID);

        // THEN
        verify(jobRepository, times(1)).findById(ID);
        verify(redisLockRegistry, never()).obtain(any());
        verify(lock, never()).tryLock();
        verify(jobRepository, never()).save(any());
        verify(lock, never()).unlock();
    }

    @Test
    void startJobWithJobLocked() {
        // GIVEN
        Job job = new Job();
        job.setId(ID);
        job.setName(NAME);
        job.setStatus(WAITING);

        Lock lock = mock(Lock.class);

        when(jobRepository.findById(ID))
                .thenReturn(Optional.of(job));
        when(redisLockRegistry.obtain(String.valueOf(ID)))
                .thenReturn(lock);
        when(lock.tryLock())
                .thenReturn(false);

        // WHEN
        assertThatThrownBy(() -> jobService.startJob(ID))
                .isInstanceOf(JobLockedException.class)
                .hasMessage("Job locked for id=" + ID);

        // THEN
        InOrder inOrder = inOrder(jobRepository, redisLockRegistry, lock);
        inOrder.verify(jobRepository, times(1)).findById(ID);
        inOrder.verify(redisLockRegistry, times(1)).obtain(String.valueOf(ID));
        inOrder.verify(lock, times(1)).tryLock();
        verify(jobRepository, never()).save(any());
        inOrder.verify(lock, never()).unlock();
    }

    private Job initJob() {
        Job job = new Job();
        job.setId(ID);
        job.setName(NAME);
        job.setStartDate(START_DATE);
        job.setEndDate(END_DATE);
        job.setStatus(STATUS);
        return job;
    }

    private void checkJobDto(JobDto job) {
        assertNotNull(job);
        assertEquals(ID, job.getId());
        assertEquals(NAME, job.getName());
        assertEquals(START_DATE, job.getStartDate());
        assertEquals(END_DATE, job.getEndDate());
        assertEquals(STATUS, job.getStatus());
    }
}
