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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 作业管理服务实现类，实现作业管理服务接口。
 */
public class JobManagerServiceImpl implements JobManagerService {
    private JobQueue jobQueue;
    private ResourceEstimator resourceEstimator;
    private ResourceManager resourceManager;
    private Queue<Job> waitingQueue; // 等待队列
    private static final int MAX_WAITING_QUEUE_SIZE; // 从配置文件读取
    private ExecutorService executorService;

    static {
        Properties properties = new Properties();
        try (InputStream input = JobManagerServiceImpl.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                MAX_WAITING_QUEUE_SIZE = 5; // 默认等待队列大小
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
        this.jobQueue = new JobQueue(loadQueueSizeFromConfig()); // 从配置文件加载队列大小
        this.resourceEstimator = new SimpleResourceEstimator(); // 默认使用简单资源评估
        this.resourceManager = ResourceManager.getInstance();
        this.waitingQueue = new LinkedList<>();
        this.executorService = Executors.newFixedThreadPool(loadQueueSizeFromConfig());
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

        if (resourceManager.allocateResources(requiredCpu, requiredMemory)) {
            jobQueue.addJob(job);
            executorService.submit(() -> executeJob(job));
            System.out.println("Process " + context.getProcessId() + " created job: " + context.getName() + " 类型 " + context.getType());
            System.out.println("剩余资源 : CPU=" + resourceManager.getAvailableCpu() + ", Memory=" + resourceManager.getAvailableMemory() + "MB");
        } else {
            System.out.println("资源不足 : " + context.getName());
            moveToWaitingQueue(job, requiredCpu, requiredMemory);
        }
        return job;
    }

    @Override
    public boolean removeJob(String jobId) {
        Job job = findJobById(jobId);
        if (job != null) {
            boolean removed = jobQueue.removeJob(jobId);
            if (removed) {
                resourceManager.releaseResources(resourceEstimator.estimateCpu(job), resourceEstimator.estimateMemory(job));
                System.out.println("移除作业 : " + jobId);
                System.out.println("移除后的剩余资源 : CPU=" + resourceManager.getAvailableCpu() + ", Memory=" + resourceManager.getAvailableMemory() + "MB");
                processWaitingQueue(); // 处理等待队列中的作业
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
        System.out.println("移除后的剩余资源 : CPU=" + resourceManager.getAvailableCpu() + ", Memory=" + resourceManager.getAvailableMemory() + "MB");
        processWaitingQueue(); // 处理等待队列中的作业
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

    // 处理等待队列的方法
    public void processWaitingQueue() {
        if (!waitingQueue.isEmpty()) {
            // 先将等待队列中的作业按优先级排序，优先级相同按作业ID排序
            List<Job> jobsToProcess = new LinkedList<>(waitingQueue);
            Collections.sort(jobsToProcess, Comparator.comparingInt((Job job) -> job.getContext().getPriority()).thenComparing((Job job) -> job.getContext().getId()));

            // 遍历排序后的作业，尝试分配资源并将其移出等待队列
            for (Job job : jobsToProcess) {
                int requiredCpu = resourceEstimator.estimateCpu(job);
                int requiredMemory = resourceEstimator.estimateMemory(job);
                if (resourceManager.allocateResources(requiredCpu, requiredMemory)) {
                    waitingQueue.remove(job);
                    jobQueue.addJob(job);
                    executorService.submit(() -> executeJob(job));
                    System.out.println("将作业从等待队列移动到作业队列 : " + job.getContext().getName());
                    System.out.println("剩余资源 : CPU=" + resourceManager.getAvailableCpu() + ", 内存=" + resourceManager.getAvailableMemory() + "MB");
                } else {
                    break; // 资源不足，停止处理等待队列
                }
            }
        }
    }

    // 执行作业的方法
    private void executeJob(Job job) {
        try {
            // 模拟作业执行时间
            Thread.sleep(job.getContext().getExecutionTime() * 1000); // 使用 executionTime
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("作业执行被中断 : " + job.getContext().getName());
        } finally {
            // 在作业执行完成后释放资源
            int requiredCpu = resourceEstimator.estimateCpu(job);
            int requiredMemory = resourceEstimator.estimateMemory(job);
            resourceManager.releaseResources(requiredCpu, requiredMemory);
            System.out.println("作业执行完成并释放资源 : " + job.getContext().getName());
            System.out.println("剩余资源 : CPU=" + resourceManager.getAvailableCpu() + ", 内存=" + resourceManager.getAvailableMemory() + "MB");
        }
    }




    private void freeUpResources(int requiredCpu, int requiredMemory) {
        // 获取当前所有作业，按优先级降序排序，优先级相同按作业ID排序
        List<Job> allJobs = jobQueue.getAllJobs();
        Collections.sort(allJobs, Comparator.comparingInt((Job job) -> job.getContext().getPriority()).reversed().thenComparing((Job job) -> job.getContext().getId()));

        // 尝试释放资源
        for (Job job : allJobs) {
            int jobCpu = resourceEstimator.estimateCpu(job);
            int jobMemory = resourceEstimator.estimateMemory(job);
            jobQueue.removeJob(job.getContext().getId());
            waitingQueue.add(job);
            resourceManager.releaseResources(jobCpu, jobMemory);
            System.out.println("通过将作业移动到等待队列来释放资源 : " + job.getContext().getName());
            System.out.println("剩余资源 : CPU=" + resourceManager.getAvailableCpu() + ", Memory=" + resourceManager.getAvailableMemory() + "MB");

            if (resourceManager.getAvailableCpu() >= requiredCpu && resourceManager.getAvailableMemory() >= requiredMemory) {
                processWaitingQueue(); // 尝试处理等待队列
                break;
            }
        }

        // 如果等待队列已满，根据优先级删除等待队列中的低优先级作业
        if (waitingQueue.size() > MAX_WAITING_QUEUE_SIZE) {
            List<Job> jobsToRemove = new LinkedList<>(waitingQueue);
            Collections.sort(jobsToRemove, Comparator.comparingInt((Job job) -> job.getContext().getPriority()).reversed().thenComparing((Job job) -> job.getContext().getId()));

            while (waitingQueue.size() > MAX_WAITING_QUEUE_SIZE) {
                Job jobToRemove = jobsToRemove.remove(0);
                waitingQueue.remove(jobToRemove);
                System.out.println("由于容量过大，已从等待队列中删除作业 : " + jobToRemove.getContext().getName());
            }
        }
    }

    private void moveToWaitingQueue(Job job, int requiredCpu, int requiredMemory) {
        List<Job> jobsToFree = new LinkedList<>(jobQueue.getAllJobs());
        Collections.sort(jobsToFree, Comparator.comparingInt((Job jobToFree) -> jobToFree.getContext().getPriority()).reversed().thenComparing((Job jobToFree) -> jobToFree.getContext().getId()));

        for (Job jobToFree : jobsToFree) {
            int jobCpu = resourceEstimator.estimateCpu(jobToFree);
            int jobMemory = resourceEstimator.estimateMemory(jobToFree);
            jobQueue.removeJob(jobToFree.getContext().getId());
            waitingQueue.add(jobToFree);
            resourceManager.releaseResources(jobCpu, jobMemory);
            System.out.println("通过将作业移动到等待队列来释放资源 : " + jobToFree.getContext().getName());
            System.out.println("剩余资源 : CPU=" + resourceManager.getAvailableCpu() + ", Memory=" + resourceManager.getAvailableMemory() + "MB");

            if (resourceManager.getAvailableCpu() >= requiredCpu && resourceManager.getAvailableMemory() >= requiredMemory) {
                break;
            }
        }

        if (resourceManager.allocateResources(requiredCpu, requiredMemory)) {
            jobQueue.addJob(job);
            executorService.submit(() -> executeJob(job));
            System.out.println("创建作业 : " + job.getContext().getName() + " 类型 " + job.getContext().getType());
            System.out.println("剩余资源 : CPU=" + resourceManager.getAvailableCpu() + ", Memory=" + resourceManager.getAvailableMemory() + "MB");
        } else {
            System.out.println("资源仍然不足，作业已添加到等待队列中 : " + job.getContext().getName());
            waitingQueue.add(job);
        }

        // 如果等待队列已满，根据优先级删除等待队列中的低优先级作业
        if (waitingQueue.size() > MAX_WAITING_QUEUE_SIZE) {
            List<Job> jobsToRemove = new LinkedList<>(waitingQueue);
            Collections.sort(jobsToRemove, Comparator.comparingInt((Job jobToRemove) -> jobToRemove.getContext().getPriority()).reversed().thenComparing((Job jobToRemove) -> jobToRemove.getContext().getId()));

            while (waitingQueue.size() > MAX_WAITING_QUEUE_SIZE) {
                Job jobToRemove = jobsToRemove.remove(0);
                waitingQueue.remove(jobToRemove);
                System.out.println("由于容量过大，已从等待队列中删除作业 : " + jobToRemove.getContext().getName());
            }
        }
    }

    private int loadQueueSizeFromConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return 10; // 默认队列大小
            }
            properties.load(input);
            return Integer.parseInt(properties.getProperty("queue.size", "10"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return 10; // 默认队列大小
        }
    }

    public List<Job> getWaitingQueue() {
        return new LinkedList<>(waitingQueue);
    }
}
