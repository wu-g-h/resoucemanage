package com.uniplore.resouce.estimator;

import com.uniplore.job.service.Job;

/**
 * 复杂资源评估类，实现资源评估接口。
 */
public class ComplexResourceEstimator implements ResourceEstimator {
    @Override
    public int estimateCpu(Job job) {
        return 4; // 复杂资源评估，假设每个作业需要4个CPU
    }

    @Override
    public int estimateMemory(Job job) {
        return 2048; // 复杂资源评估，假设每个作业需要2048MB内存
    }
}
