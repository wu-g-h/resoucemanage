import com.uniplore.job.JobContext;
import com.uniplore.job.service.Job;
import com.uniplore.job.service.JobManagerService;
import com.uniplore.job.service.impl.JobManagerServiceImpl;
import com.uniplore.resouce.estimator.AdvancedResourceEstimator;
import com.uniplore.resouce.estimator.ComplexResourceEstimator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

/**
 * 作业管理服务的单元测试类。
 */
public class JobManagerServiceTest {

    @Test
    public void testCreateAndRemoveJob() {
        JobManagerService jobManager = new JobManagerServiceImpl();

        JobContext context = new JobContext("1", "Test Job", "User1", 1, "General", "Content of the job", 1, 5);
        Job job = jobManager.createJob(context);
        assertNotNull(job);

        boolean removed = jobManager.removeJob("1");
        assertTrue(removed);
    }

    @Test
    public void testEstimateResources() {
        JobManagerService jobManager = new JobManagerServiceImpl();
        ((JobManagerServiceImpl) jobManager).setResourceEstimator(new ComplexResourceEstimator());

        JobContext context = new JobContext("2", "Test Job", "User1", 1, "General", "Content of the job", 1, 5);
        jobManager.createJob(context);

        jobManager.estimateResources("2");
    }

    @Test
    public void testAdvancedJobEstimation() {
        JobManagerService jobManager = new JobManagerServiceImpl();
        ((JobManagerServiceImpl) jobManager).setResourceEstimator(new AdvancedResourceEstimator());

        JobContext context = new JobContext("3", "Advanced Job", "User1", 1, "General", "Advanced job content", 1, 10);
        jobManager.createJob(context);

        jobManager.estimateResources("3");
    }

    @Test
    public void testGetAllJobs() {
        JobManagerService jobManager = new JobManagerServiceImpl();

        JobContext context1 = new JobContext("1", "Test Job 1", "User1", 1, "General", "Content of the job 1", 1, 5);
        JobContext context2 = new JobContext("2", "Test Job 2", "User2", 2, "General", "Content of the job 2", 1, 5);

        jobManager.createJob(context1);
        jobManager.createJob(context2);

        List<Job> allJobs = jobManager.getAllJobs();
        assertEquals(2, allJobs.size());
    }

    @Test
    public void testGetJobsByUser() {
        JobManagerService jobManager = new JobManagerServiceImpl();
        ((JobManagerServiceImpl) jobManager).setResourceEstimator(new ComplexResourceEstimator());

        JobContext context1 = new JobContext("1", "Test Job 1", "User1", 1, "General", "Content of the job 1", 1, 5);
        JobContext context2 = new JobContext("2", "Test Job 2", "User1", 20, "General", "Content of the job 2", 1, 5);
        JobContext context3 = new JobContext("3", "Test Job 3", "User1", 25, "General", "Content of the job 2", 1, 5);
        JobContext context4 = new JobContext("4", "Test Job 4", "User1", 12, "General", "Content of the job 2", 1, 5);
        JobContext context5 = new JobContext("5", "Test Job 5", "User1", 2, "General", "Content of the job 2", 1, 5);
        JobContext context6 = new JobContext("6", "Test Job 6", "User1", 29, "General", "Content of the job 2", 1, 5);
        JobContext context7 = new JobContext("7", "Test Job 7", "User1", 23, "General", "Content of the job 2", 1, 5);
        JobContext context8 = new JobContext("7", "Test Job 7", "User1", 23, "General", "Content of the job 2", 1, 5);
        JobContext context9 = new JobContext("7", "Test Job 7", "User1", 23, "General", "Content of the job 2", 1, 5);
        JobContext context10 = new JobContext("7", "Test Job 7", "User1", 23, "General", "Content of the job 2", 1, 5);
        JobContext context11 = new JobContext("7", "Test Job 7", "User1", 23, "General", "Content of the job 2", 1, 5);

        Job job1 = jobManager.createJob(context1);
        System.out.println("Created job of type: " + job1.getClass().getSimpleName());
        Job job2 = jobManager.createJob(context2);
        Job job3 = jobManager.createJob(context3);
        Job job4 = jobManager.createJob(context4);
        Job job5 = jobManager.createJob(context5);
        Job job6 = jobManager.createJob(context6);
        Job job7 = jobManager.createJob(context7);
        Job job8 = jobManager.createJob(context7);
        Job job9 = jobManager.createJob(context7);
        Job job10 = jobManager.createJob(context7);
        Job job11 = jobManager.createJob(context7);
        System.out.println("Created job of type: " + job2.getClass().getSimpleName());

        List<Job> user1Jobs = jobManager.getJobsByUser("User1");
        assertEquals(7, user1Jobs.size());
        System.out.println("Jobs for User1: " + user1Jobs);
    }

    @Test
    public void testUpdateJob() {
        JobManagerService jobManager = new JobManagerServiceImpl();

        JobContext context = new JobContext("1", "Test Job", "User1", 1, "General", "Content of the job", 1, 5);
        jobManager.createJob(context);

        boolean updated = jobManager.updateJob("1", "Updated Test Job", "Updated content of the job");
        assertTrue(updated);

        List<Job> allJobs = jobManager.getAllJobs();
        assertEquals("Updated Test Job", allJobs.get(0).getContext().getName());
        assertEquals("Updated content of the job", allJobs.get(0).getContext().getContent());
    }

    @Test
    public void testRemoveJobs() {
        JobManagerService jobManager = new JobManagerServiceImpl();

        JobContext context1 = new JobContext("1", "Test Job 1", "User1", 1, "General", "Content of the job 1", 1, 5);
        JobContext context2 = new JobContext("2", "Test Job 2", "User2", 2, "General", "Content of the job 2", 1, 5);

        jobManager.createJob(context1);
        jobManager.createJob(context2);

        boolean removed = jobManager.removeJobs(List.of("1", "2"));
        assertTrue(removed);

        List<Job> allJobs = jobManager.getAllJobs();
        assertEquals(0, allJobs.size());
    }

    @Test
    public void testWaitingQueueOverflow() {
        JobManagerService jobManager = new JobManagerServiceImpl();
        ((JobManagerServiceImpl) jobManager).setResourceEstimator(new ComplexResourceEstimator());

        for (int i = 1; i <= 20; i++) {
            JobContext context = new JobContext(String.valueOf(i), "Test Job " + i, "User1", i, "General", "Content of the job " + i, 1, 5);
            jobManager.createJob(context);
        }

        List<Job> allJobs = jobManager.getAllJobs();
        assertEquals(10, allJobs.size());

        // 验证等待队列是否正常工作
        List<Job> waitingJobs = ((JobManagerServiceImpl) jobManager).getWaitingQueue();
        assertEquals(10, waitingJobs.size());

        // 移除一个作业以触发等待队列的处理
        boolean removed = jobManager.removeJob("1");
        assertTrue(removed);

        allJobs = jobManager.getAllJobs();
        assertEquals(10, allJobs.size());
    }
}
