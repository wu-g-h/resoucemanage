package com.uniplore.job.service.impl;

import com.uniplore.job.JobContext;
import com.uniplore.job.service.Job;

/**
 * 通用作业类，实现作业接口。
 */
public class GeneralJob implements Job {
    private JobContext context;

    public GeneralJob(JobContext context) {
        this.context = context;
    }

    @Override
    public JobContext getContext() {
        return context;
    }
}
