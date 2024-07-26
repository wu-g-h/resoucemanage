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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class JobManagerServiceImpl implements JobManagerService {
    private JobQueue jobQueue;
    private ResourceEstimator resourceEstimator;
    private ResourceManager resourceManager;
    private ExecutorService executorService;

    public JobManagerServiceImpl() {
        this.jobQueue = new JobQueue(loadQueueSizeFromConfig());
        this.resourceEstimator = new SimpleResourceEstimator();
        this.resourceManager = ResourceManager.getInstance();
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
        synchronized (jobQueue) {
            if (!jobQueue.addJob(job)) {
                System.out.println("工作队列已满，尝试强制回收资源: " + job.getContext().getName());
                if (forceRemoveRunningJob()) {
                    jobQueue.forceAddJob(job);
                } else {
                    System.out.println("工作队列已满，无法添加作业: " + job.getContext().getName());
                    return job; // 确保在无法添加作业时返回
                }
            }
            processJobQueue();
        }
        System.out.println("Process " + context.getProcessId() + " created job: " + context.getName() + " 类型 " + context.getType());
        return job;
    }

    private void processJobQueue() {
        synchronized (jobQueue) {
            Job job = jobQueue.peekJob();
            while (job != null) {
                int requiredCpu = resourceEstimator.estimateCpu(job);
                int requiredMemory = resourceEstimator.estimateMemory(job);
                synchronized (resourceManager) {
                    if (resourceManager.allocateResources(job, requiredCpu, requiredMemory)) {
                        jobQueue.pollJob();
                        Job finalJob = job;
                        Future<?> future = executorService.submit(() -> executeJob(finalJob));
                        job.getContext().setFuture(future);
                        System.out.println("作业开始执行 : " + job.getContext().getName());
                    } else {
                        break;
                    }
                }
                job = jobQueue.peekJob();
            }
        }
    }

    private void executeJob(Job job) {
        int requiredCpu = resourceEstimator.estimateCpu(job);
        int requiredMemory = resourceEstimator.estimateMemory(job);
        try {
            Thread.sleep(job.getContext().getExecutionTime() * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("作业执行被中断 : " + job.getContext().getName());
        } finally {
            resourceManager.releaseResources(job, requiredCpu, requiredMemory);
            System.out.println("作业执行完成并释放资源 : " + job.getContext().getName());
            processJobQueue();
        }
    }

    @Override
    public boolean removeJob(String jobId) {
        synchronized (jobQueue) {
            Job job = findJobById(jobId);
            int requiredCpu = resourceEstimator.estimateCpu(job);
            int requiredMemory = resourceEstimator.estimateMemory(job);
            if (job != null) {
                Future<?> future = job.getContext().getFuture();
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }
                boolean removed = jobQueue.removeJob(jobId);
                if (removed) {
                    resourceManager.releaseResources(job, requiredCpu, requiredMemory);
                    System.out.println("移除作业 : " + jobId);
                    processJobQueue();
                    return true;
                }
            }
            System.out.println("未找到该作业 : " + jobId);
            return false;
        }
    }

    @Override
    public boolean removeJobs(List<String> jobIds) {
        synchronized (jobQueue) {
            for (String jobId : jobIds) {
                removeJob(jobId);
            }
            return true;
        }
    }

    @Override
    public List<Job> getAllJobs() {
        return jobQueue.getAllJobs();
    }

    @Override
    public List<Job> getJobsByUser(String user) {
        return jobQueue.getJobsByUser(user);
    }

    @Override
    public List<Job> getJobsByType(String type) {
        return jobQueue.getJobsByType(type);
    }

    @Override
    public void estimateResources(String jobId) {
        Job job = findJobById(jobId);
        if (job != null) {
            int cpu = resourceEstimator.estimateCpu(job);
            int memory = resourceEstimator.estimateMemory(job);
            System.out.println("作业 " + jobId + " 需要资源: CPU=" + cpu + ", 内存=" + memory + "MB");
        } else {
            System.out.println("未找到该作业 : " + jobId);
        }
    }

    private Job findJobById(String jobId) {
        return jobQueue.getAllJobs().stream()
                .filter(job -> job.getContext().getId().equals(jobId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean updateJob(String jobId, String newName, String newContent) {
        Job job = findJobById(jobId);
        if (job != null) {
            job.getContext().setName(newName);
            job.getContext().setContent(newContent);
            System.out.println("更新作业 : " + jobId + " 新名称: " + newName + " 新内容: " + newContent);
            return true;
        } else {
            System.out.println("未找到该作业 : " + jobId);
            return false;
        }
    }

    @Override
    public void processWaitingQueue() {
        processJobQueue();
    }

    private boolean forceRemoveRunningJob() {
        List<Job> runningJobs = resourceManager.getRunningJobs();
        if (!runningJobs.isEmpty()) {
            Job job = runningJobs.get(0); // 只移除第一个正在运行的作业
            Future<?> future = job.getContext().getFuture();
            if (future != null && !future.isDone()) {
                future.cancel(true);
//                resourceManager.releaseResources(job);
                System.out.println("强制移除作业 : " + job.getContext().getName());
                return true;
            }
        }
        return false;
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
}
