package com.uniplore.job.service.impl;

import com.uniplore.job.JobContext;
import com.uniplore.job.service.Job;

/**
 * 简单作业类，实现作业接口。
 */
public class SimpleJob implements Job {
    private JobContext context;

    public SimpleJob(JobContext context) {
        this.context = context;
    }

    @Override
    public JobContext getContext() {
        return context;
    }
}
