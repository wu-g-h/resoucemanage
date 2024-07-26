package com.uniplore.resouce.management;

import com.uniplore.job.service.Job;
import com.uniplore.job.service.JobManagerService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 资源管理类，管理所有资源。
 */
public class ResourceManager {
    private int totalCpu;
    private int totalMemory;
    private int availableCpu;
    private int availableMemory;
    private static ResourceManager instance;
    private static final Object lock = new Object();
    private final Lock resourceLock = new ReentrantLock();
    private JobManagerService jobManagerService;
    private List<Job> runningJobs;

    private ResourceManager() {
        loadResourceConfig();
        runningJobs = new ArrayList<>();
    }

    public static ResourceManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ResourceManager();
                }
            }
        }
        return instance;
    }

    public void setJobManagerService(JobManagerService jobManagerService) {
        this.jobManagerService = jobManagerService;
    }

    private void loadResourceConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }
            properties.load(input);
            this.totalCpu = Integer.parseInt(properties.getProperty("cpu.total", "20"));
            this.totalMemory = Integer.parseInt(properties.getProperty("memory.total", "20480"));
            this.availableCpu = totalCpu;
            this.availableMemory = totalMemory;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean allocateResources(Job job, int cpu, int memory) {
        resourceLock.lock();
        try {
            if (availableCpu >= cpu && availableMemory >= memory) {
                availableCpu -= cpu;
                availableMemory -= memory;
                runningJobs.add(job);
                System.out.println("分配资源 : CPU=" + cpu + ", Memory=" + memory + "MB");
                System.out.println("剩余资源 : CPU=" + availableCpu + ", Memory=" + availableMemory + "MB");
                return true;
            } else {
                System.out.println("资源不足，无法分配资源");
                return false;
            }
        } finally {
            resourceLock.unlock();
        }
    }

    public void releaseResources(Job job, int cpu, int memory) {
        resourceLock.lock();
        try {
            availableCpu = Math.min(totalCpu, availableCpu + cpu);
            availableMemory = Math.min(totalMemory, availableMemory + memory);
            runningJobs.remove(job);
            System.out.println("释放资源 : CPU=" + cpu + ", 内存=" + memory + "MB");
            System.out.println("剩余资源 : CPU=" + availableCpu + ", 内存=" + availableMemory + "MB");
        } finally {
            resourceLock.unlock();
        }
        jobManagerService.processWaitingQueue();
    }

    public List<Job> getRunningJobs() {
        return new ArrayList<>(runningJobs);
    }

    public synchronized int getAvailableCpu() {
        return availableCpu;
    }

    public synchronized int getAvailableMemory() {
        return availableMemory;
    }

    public synchronized int getAllCpu() {
        return totalCpu;
    }

    public synchronized int getAllMemory() {
        return totalMemory;
    }

    private void checkResourceWarning() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }
            properties.load(input);
            int cpuThreshold = Integer.parseInt(properties.getProperty("resource.warning.threshold", "95"));
            int memoryThreshold = Integer.parseInt(properties.getProperty("resource.warning.threshold", "95"));

            int usedCpuPercentage = (int) ((1 - ((double) availableCpu / totalCpu)) * 100);
            int usedMemoryPercentage = (int) ((1 - ((double) availableMemory / totalMemory)) * 100);

            if (usedCpuPercentage >= cpuThreshold || usedMemoryPercentage >= memoryThreshold) {
                System.out.println("资源警告 : Available CPU=" + availableCpu + ", Available Memory=" + availableMemory + "MB");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
