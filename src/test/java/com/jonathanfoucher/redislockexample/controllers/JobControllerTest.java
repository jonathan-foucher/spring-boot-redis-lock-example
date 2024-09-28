package com.jonathanfoucher.redislockexample.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jonathanfoucher.redislockexample.controllers.advices.GlobalControllerExceptionHandler;
import com.jonathanfoucher.redislockexample.data.dto.JobDto;
import com.jonathanfoucher.redislockexample.data.enums.JobStatus;
import com.jonathanfoucher.redislockexample.errors.JobAlreadyProcessedException;
import com.jonathanfoucher.redislockexample.errors.JobLockedException;
import com.jonathanfoucher.redislockexample.errors.JobNotFoundException;
import com.jonathanfoucher.redislockexample.services.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@SpringJUnitConfig({JobController.class, GlobalControllerExceptionHandler.class})
class JobControllerTest {
    private MockMvc mockMvc;
    @Autowired
    private JobController jobController;
    @Autowired
    private GlobalControllerExceptionHandler globalControllerExceptionHandler;
    @MockBean
    private JobService jobService;

    private static final String JOB_PATH = "/jobs";
    private static final String START_JOB_PATH = "/jobs/{id}/start";
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

    @BeforeEach
    void initEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(jobController)
                .setControllerAdvice(globalControllerExceptionHandler)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void getAllJobs() throws Exception {
        // GIVEN
        JobDto job = initJobDto();

        when(jobService.getAllJobs())
                .thenReturn(List.of(job));

        // WHEN / THEN
        mockMvc.perform(get(JOB_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(job))));

        verify(jobService, times(1)).getAllJobs();
    }

    @Test
    void getAllJobsWithEmptyResult() throws Exception {
        // GIVEN
        when(jobService.getAllJobs())
                .thenReturn(emptyList());

        // WHEN / THEN
        mockMvc.perform(get(JOB_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(emptyList())));

        verify(jobService, times(1)).getAllJobs();
    }

    @Test
    void createJob() throws Exception {
        // GIVEN
        when(jobService.createJob(NAME))
                .thenReturn(ID);

        // WHEN / THEN
        mockMvc.perform(post(JOB_PATH)
                        .queryParam("name", NAME))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(ID)));

        verify(jobService, times(1)).createJob(NAME);
    }

    @Test
    void startJob() throws Exception {
        // WHEN / THEN
        mockMvc.perform(post(START_JOB_PATH, ID))
                .andExpect(status().isOk());

        verify(jobService, times(1)).startJob(ID);
    }

    @Test
    void startJobWithJobNotFound() throws Exception {
        // GIVEN
        doThrow(new JobNotFoundException(ID))
                .when(jobService).startJob(ID);

        // WHEN / THEN
        mockMvc.perform(post(START_JOB_PATH, ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string("\"Job not found for id=" + ID + "\""));

        verify(jobService, times(1)).startJob(ID);
    }

    @Test
    void startJobWithJobAlreadyProcessed() throws Exception {
        // GIVEN
        doThrow(new JobAlreadyProcessedException(ID))
                .when(jobService).startJob(ID);

        // WHEN / THEN
        mockMvc.perform(post(START_JOB_PATH, ID))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("\"Job already processed for id=" + ID + "\""));

        verify(jobService, times(1)).startJob(ID);
    }

    @Test
    void startJobWithJobLocked() throws Exception {
        // GIVEN
        doThrow(new JobLockedException(ID))
                .when(jobService).startJob(ID);

        // WHEN / THEN
        mockMvc.perform(post(START_JOB_PATH, ID))
                .andExpect(status().isLocked())
                .andExpect(content().string("\"Job locked for id=" + ID + "\""));

        verify(jobService, times(1)).startJob(ID);
    }

    private JobDto initJobDto() {
        JobDto job = new JobDto();
        job.setId(ID);
        job.setName(NAME);
        job.setStartDate(START_DATE);
        job.setEndDate(END_DATE);
        job.setStatus(STATUS);
        return job;
    }
}
