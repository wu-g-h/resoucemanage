package com.uniplore.resouce.estimator;

import com.uniplore.job.service.Job;

/**
 * 资源评估接口，定义资源评估方法。
 */
public interface ResourceEstimator {
    int estimateCpu(Job job);
    int estimateMemory(Job job);
}
