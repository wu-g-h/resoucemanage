package com.uniplore.job.service;

import com.uniplore.job.JobContext;

import java.util.List;

/**
 * 作业管理服务接口，定义作业管理服务的方法。
 */
public interface JobManagerService {
    Job createJob(JobContext context);
    boolean removeJob(String jobId);
    boolean removeJobs(List<String> jobIds);
    void estimateResources(String jobId);
    List<Job> getAllJobs();
    List<Job> getJobsByUser(String user);
    List<Job> getJobsByType(String type);
    boolean updateJob(String jobId, String newName, String newContent);
    void processWaitingQueue();
}
