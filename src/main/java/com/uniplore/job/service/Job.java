package com.uniplore.job.service;

import com.uniplore.job.JobContext;

/**
 * 作业接口，定义所有作业的基础方法。
 */
public interface Job {
    JobContext getContext();

    int getPriority();


    int getCpuUsage();

    int getMemoryUsage();

    String getId();
}
