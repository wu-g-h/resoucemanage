package com.uniplore.job.service.impl;

import com.uniplore.factory.JobFactory;
import com.uniplore.job.JobContext;
import com.uniplore.job.service.Job;
import com.uniplore.job.service.JobManagerService;
import com.uniplore.queue.JobQueue;
import com.uniplore.resouce.estimator.ResourceEstimator;
import com.uniplore.resouce.estimator.SimpleResourceEstimator;
import com.uniplore.resouce.management.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobManagerServiceImpl implements JobManagerService {
    private JobQueue jobQueue;
    private ResourceEstimator resourceEstimator;
    private ResourceManager resourceManager;
    private Queue<Job> waitingQueue;
    private static final int MAX_WAITING_QUEUE_SIZE;
    private ExecutorService executorService;

    static {
        Properties properties = new Properties();
        try (InputStream input = JobManagerServiceImpl.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                MAX_WAITING_QUEUE_SIZE = 5;
            } else {
                properties.load(input);
                MAX_WAITING_QUEUE_SIZE = Integer.parseInt(properties.getProperty("waiting.queue.size", "5"));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to load configuration");
        }
    }

    public JobManagerServiceImpl() {
        this.jobQueue = new JobQueue(loadQueueSizeFromConfig());
        this.resourceEstimator = new SimpleResourceEstimator();
        this.resourceManager = ResourceManager.getInstance();
        this.waitingQueue = new LinkedList<>();
        this.executorService = Executors.newFixedThreadPool(loadQueueSizeFromConfig());
        this.resourceManager.setJobManagerService(this);
    }

    public void setResourceEstimator(ResourceEstimator resourceEstimator) {
        this.resourceEstimator = resourceEstimator;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    @Override
    public Job createJob(JobContext context) {
        Job job = JobFactory.createJob(context);
        int requiredCpu = resourceEstimator.estimateCpu(job);
        int requiredMemory = resourceEstimator.estimateMemory(job);

        synchronized (resourceManager) {
            if (resourceManager.allocateResources(requiredCpu, requiredMemory)) {
                jobQueue.addJob(job);
                executorService.submit(() -> executeJob(job));
                System.out.println("Process " + context.getProcessId() + " created job: " + context.getName() + " 类型 " + context.getType());
            } else {
                System.out.println("资源不足 : " + context.getName());
                addToWaitingQueue(job);
            }
        }
        return job;
    }

    private void addToWaitingQueue(Job job) {
        synchronized (waitingQueue) {
            if (waitingQueue.size() < MAX_WAITING_QUEUE_SIZE) {
                waitingQueue.add(job);
                System.out.println("作业已添加到等待队列中 : " + job.getContext().getName());
            } else {
                forceRemoveJob();
                if (waitingQueue.size() < MAX_WAITING_QUEUE_SIZE) {
                    waitingQueue.add(job);
                    System.out.println("作业已添加到等待队列中 : " + job.getContext().getName());
                } else {
                    System.out.println("等待队列已满，作业无法添加 : " + job.getContext().getName());
                }
            }
        }
    }

    @Override
    public boolean removeJob(String jobId) {
        Job job = findJobById(jobId);
        if (job != null) {
            boolean removed = jobQueue.removeJob(jobId);
            if (removed) {
                resourceManager.releaseResources(resourceEstimator.estimateCpu(job), resourceEstimator.estimateMemory(job));
                System.out.println("移除作业 : " + jobId);
                processWaitingQueue();
                return true;
            }
        }
        System.out.println("未找到该作业 : " + jobId);
        return false;
    }

    @Override
    public boolean removeJobs(List<String> jobIds) {
        for (String jobId : jobIds) {
            Job job = findJobById(jobId);
            if (job != null) {
                jobQueue.removeJob(jobId);
                resourceManager.releaseResources(resourceEstimator.estimateCpu(job), resourceEstimator.estimateMemory(job));
            }
        }
        System.out.println("移除作业 : " + jobIds);
        processWaitingQueue();
        return true;
    }

    @Override
    public void estimateResources(String jobId) {
        Job job = jobQueue.pollJob();
        if (job != null && job.getContext().getId().equals(jobId)) {
            resourceEstimator.estimateCpu(job);
            resourceEstimator.estimateMemory(job);
        } else {
            System.out.println("未找到该作业 : " + jobId);
        }
    }

    @Override
    public List<Job> getAllJobs() {
        List<Job> jobs = jobQueue.getAllJobs();
        System.out.println("全部作业 : " + jobs);
        return jobs;
    }

    @Override
    public List<Job> getJobsByUser(String user) {
        List<Job> jobs = jobQueue.getJobsByUser(user);
        System.out.println("作业属于 " + user + ": " + jobs);
        return jobs;
    }

    @Override
    public List<Job> getJobsByType(String type) {
        List<Job> jobs = jobQueue.getJobsByType(type);
        System.out.println("作业类型 " + type + ": " + jobs);
        return jobs;
    }

    @Override
    public boolean updateJob(String jobId, String newName, String newContent) {
        for (Job job : jobQueue.getAllJobs()) {
            if (job.getContext().getId().equals(jobId)) {
                job.getContext().setName(newName);
                job.getContext().setContent(newContent);
                System.out.println("更新作业 : " + job.getContext().getName());
                return true;
            }
        }
        System.out.println("未找到该作业 : " + jobId);
        return false;
    }

    private Job findJobById(String jobId) {
        for (Job job : jobQueue.getAllJobs()) {
            if (job.getContext().getId().equals(jobId)) {
                return job;
            }
        }
        return null;
    }

    public void processWaitingQueue() {
        synchronized (waitingQueue) {
            if (!waitingQueue.isEmpty()) {
                List<Job> jobsToProcess = new LinkedList<>(waitingQueue);
                Collections.sort(jobsToProcess, Comparator.comparingInt((Job job) -> job.getContext().getPriority()).thenComparing((Job job) -> job.getContext().getId()));

                Iterator<Job> iterator = jobsToProcess.iterator();
                while (iterator.hasNext()) {
                    Job job = iterator.next();
                    int requiredCpu = resourceEstimator.estimateCpu(job);
                    int requiredMemory = resourceEstimator.estimateMemory(job);
                    synchronized (resourceManager) {
                        if (resourceManager.allocateResources(requiredCpu, requiredMemory)) {
                            waitingQueue.remove(job);
                            jobQueue.addJob(job);
                            executorService.submit(() -> executeJob(job));
                            System.out.println("将作业从等待队列移动到作业队列 : " + job.getContext().getName());
                            iterator.remove();
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void executeJob(Job job) {
        try {
            Thread.sleep(job.getContext().getExecutionTime() * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("作业执行被中断 : " + job.getContext().getName());
        } finally {
            int requiredCpu = resourceEstimator.estimateCpu(job);
            int requiredMemory = resourceEstimator.estimateMemory(job);
            synchronized (resourceManager) {
                resourceManager.releaseResources(requiredCpu, requiredMemory);
            }
            System.out.println("作业执行完成并释放资源 : " + job.getContext().getName());
            processWaitingQueue();
        }
    }

    private int loadQueueSizeFromConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return 10;
            }
            properties.load(input);
            return Integer.parseInt(properties.getProperty("queue.size", "10"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return 10;
        }
    }

    public List<Job> getWaitingQueue() {
        return new LinkedList<>(waitingQueue);
    }

    private void forceRemoveJob() {
        List<Job> jobs = jobQueue.getAllJobs();
        jobs.sort(Comparator.comparingInt(Job::getPriority).reversed());
        Job jobToForceRemove = jobs.get(0);
        resourceManager.releaseResources(resourceEstimator.estimateCpu(jobToForceRemove), resourceEstimator.estimateMemory(jobToForceRemove));
        jobQueue.removeJob(jobToForceRemove.getId());
        System.out.println("强制移除作业 : " + jobToForceRemove.getContext().getName());
    }
}
