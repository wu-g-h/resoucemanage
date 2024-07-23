package com.uniplore.resouce.estimator;

import com.uniplore.job.service.Job;


/**
 * @author: wuguihua
 * @date: 2024/07/23 13:16
 * @desc: 高级资源评估类，实现资源评估接口。
 */

public class AdvancedResourceEstimator implements ResourceEstimator {
    @Override
    public int estimateCpu(Job job) {
        return 8; // 高级资源评估，假设每个作业需要8个CPU
    }

    @Override
    public int estimateMemory(Job job) {
        return 4096; // 高级资源评估，假设每个作业需要4096MB内存
    }
}
