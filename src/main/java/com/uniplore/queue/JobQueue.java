package com.uniplore.queue;

import com.uniplore.job.service.Job;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author: wuguihua
 * @date: 2024/07/23 13:16
 * @desc: 作业队列类，管理所有作业队列。
 */

public class JobQueue {
    private Queue<Job> jobQueue;
    private int maxQueueSize;

    public JobQueue(int maxQueueSize) {
        this.jobQueue = new LinkedList<>();
        this.maxQueueSize = maxQueueSize;
    }

    public int size() {
        return jobQueue.size();
    }

    public boolean addJob(Job job) {
        if (jobQueue.size() < maxQueueSize) {
            return jobQueue.offer(job);
        } else {
            System.out.println("工作队列已满，进入等待队列等待: " + job.getContext().getName());
            return false;
        }
    }

    public boolean removeJob(String jobId) {
        return jobQueue.removeIf(job -> job.getContext().getId().equals(jobId));
    }

    public boolean removeJobs(List<String> jobIds) {
        return jobQueue.removeIf(job -> jobIds.contains(job.getContext().getId()));
    }

    public List<Job> getAllJobs() {
        return new LinkedList<>(jobQueue);
    }

    public List<Job> getJobsByUser(String user) {
        List<Job> jobs = new LinkedList<>();
        for (Job job : jobQueue) {
            if (job.getContext().getUser().equals(user)) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    public List<Job> getJobsByType(String type) {
        List<Job> jobs = new LinkedList<>();
        for (Job job : jobQueue) {
            if (job.getContext().getType().equals(type)) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    public Job pollJob() {
        return jobQueue.poll();
    }
}
