package com.uniplore.job.service.impl;

import com.uniplore.job.JobContext;
import com.uniplore.job.service.Job;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 通用作业类，实现作业接口。
 */
public class SimpleJob implements Job {
    private JobContext context;
    private int cpuUsage;
    private int memoryUsage;

    public SimpleJob(JobContext context) {
        this.context = context;
        // 从配置文件中读取 CPU 和内存的值
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IOException("Sorry, unable to find config.properties");
            }
            properties.load(input);
            this.cpuUsage = Integer.parseInt(properties.getProperty("cpu.total", "20"));
            this.memoryUsage = Integer.parseInt(properties.getProperty("memory.total", "20480"));
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JobContext getContext() {
        return context;
    }

    @Override
    public int getPriority() {
        return context.getPriority();
    }

    @Override
    public int getCpuUsage() {
        return cpuUsage;
    }

    @Override
    public int getMemoryUsage() {
        return memoryUsage;
    }

    @Override
    public String getId() {
        return context.getId();
    }
}
