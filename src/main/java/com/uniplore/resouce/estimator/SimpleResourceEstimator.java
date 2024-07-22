package com.uniplore.resouce.estimator;

import com.uniplore.job.service.Job;

/**
 * 简单资源评估类，实现资源评估接口。
 */
public class SimpleResourceEstimator implements ResourceEstimator {
    @Override
    public int estimateCpu(Job job) {
        return 2; // 简单资源评估，假设每个作业需要2个CPU
    }

    @Override
    public int estimateMemory(Job job) {
        return 1024; // 简单资源评估，假设每个作业需要1024MB内存
    }
}
