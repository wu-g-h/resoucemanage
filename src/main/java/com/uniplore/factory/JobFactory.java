package com.uniplore.factory;

import com.uniplore.job.JobContext;
import com.uniplore.job.service.Job;
import com.uniplore.job.service.impl.GeneralJob;
import com.uniplore.job.service.impl.SimpleJob;

/**
 * @author: wuguihua
 * @date: 2024/07/23 13:16
 * @desc: 作业工厂类，创建不同类型的作业实例。
 */

public class JobFactory {
    public static Job createJob(JobContext context) {
        switch (context.getType()) {
            case "General":
                return new GeneralJob(context);
            case "Simple":
                return new SimpleJob(context);
            default:
                throw new IllegalArgumentException("Unknown job type: " + context.getType());
        }
    }
}
