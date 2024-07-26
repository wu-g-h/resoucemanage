import com.uniplore.job.JobContext;
import com.uniplore.job.service.Job;
import com.uniplore.job.service.JobManagerService;
import com.uniplore.job.service.impl.JobManagerServiceImpl;
import com.uniplore.resouce.estimator.ComplexResourceEstimator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultiProcessTest {

    @Test
    public void testMultiProcessJobCreation() throws InterruptedException {
        JobManagerService jobManager = new JobManagerServiceImpl();
        ((JobManagerServiceImpl) jobManager).setResourceEstimator(new ComplexResourceEstimator());

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        for (int i = 1; i <= 17; i++) {
            int finalI = i;
            executorService.submit(() -> {
                JobContext context = new JobContext(
                        String.valueOf(finalI),
                        "测试作业 " + finalI,
                        "用户" + finalI,
                        finalI,
                        "General",
                        "作业内容 " + finalI,
                        finalI,  // Process ID
                        1  // 执行时间（秒）
                );
                Job job = jobManager.createJob(context);
                if (job != null) {
                    System.out.println("创建作业类型: " + job.getClass().getSimpleName() + " 由进程 " + finalI + " 创建");
                }
            });
        }

        executorService.awaitTermination(30, TimeUnit.SECONDS);
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.sleep(10000);
        }
    }
}
