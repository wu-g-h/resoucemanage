//package com.uniplore.job.service.impl;
//
//import com.uniplore.job.JobContext;
//import com.uniplore.job.service.Job;
//import com.uniplore.job.service.JobManagerService;
//
//public class JobProcess {
//
//    public static void main(String[] args) {
//        int processId = Integer.parseInt(args[0]);
//        JobManagerService jobManager = new JobManagerServiceImpl();
//
//        // 模拟创建多个作业
//        for (int i = 1; i <= 10; i++) {
//            JobContext context = new JobContext(String.valueOf(i + processId * 100), "Test Job " + (i + processId * 100), "User" + processId, i, "General", "Content of the job " + (i + processId * 100));
//            Job job = jobManager.createJob(context);
//            System.out.println("Process " + processId + " created job: " + job.getContext().getName());
//        }
//
//        // 模拟移除作业
//        for (int i = 1; i <= 5; i++) {
//            jobManager.removeJob(String.valueOf(i + processId * 100));
//            System.out.println("Process " + processId + " removed job: " + (i + processId * 100));
//        }
//
//        // 打印剩余作业
//        jobManager.getAllJobs().forEach(job -> System.out.println("Remaining job: " + job.getContext().getName()));
//    }
//}
