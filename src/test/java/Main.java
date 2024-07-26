import com.uniplore.job.JobContext;
import com.uniplore.job.service.JobManagerService;
import com.uniplore.job.service.impl.JobManagerServiceImpl;
import com.uniplore.resouce.estimator.ComplexResourceEstimator;

public class Main {
    public static void main(String[] args) {
        JobManagerService jobManager = new JobManagerServiceImpl();
        ((JobManagerServiceImpl) jobManager).setResourceEstimator(new ComplexResourceEstimator());
        for (int i = 1; i <= 16; i++) {
            JobContext context = new JobContext(String.valueOf(i), "测试作业 " + i, "user" + i, 1, "General", "Content", i, 2);
            jobManager.createJob(context);
        }

        JobContext context17 = new JobContext("17", "测试作业 17", "user17", 1, "General", "Content", 17, 1);
        jobManager.createJob(context17);
    }
}
