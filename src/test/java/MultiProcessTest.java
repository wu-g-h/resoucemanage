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
        // 设置复杂的资源评估器
        ((JobManagerServiceImpl) jobManager).setResourceEstimator(new ComplexResourceEstimator());

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 1; i <= 15; i++) {
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

        // 等待一段时间，让作业执行
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // 检查等待队列是否满了，然后检查是否删除了正在进行的作业
        // 这里需要根据您的作业管理系统实现的具体逻辑来添加检查代码
        // 例如，您可以检查等待队列的大小，然后检查工作队列中是否有作业被移除

        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.sleep(1000);
        }
    }
}
