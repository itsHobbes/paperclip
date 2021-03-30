package uk.co.markg.paperclip.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import javax.security.auth.login.LoginException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class TaskManagerTest {

    private static JDA jda;
    private static final String TASK_PACKAGE = "uk.co.markg.paperclip.task";

    @BeforeAll
    public static void init() {
        try {
            jda = JDABuilder.createLight(System.getenv("B_TOKEN")).build();
        } catch (LoginException e) {
            fail(e);
        }
    }

    @Test
    public void getInstanceIsNotNull() {
        TaskManager manager = TaskManager.getInstance(jda, TASK_PACKAGE);
        assertNotNull(manager);
    }

    @Test
    public void getRunningTasks() {
        TaskManager manager = TaskManager.getInstance(jda, TASK_PACKAGE);
        var tasks = manager.getRunningTasks();
        assertTrue(tasks.size() == 1);
    }

    @Test
    public void getStoppedTasks() {
        TaskManager manager = TaskManager.getInstance(jda, TASK_PACKAGE);
        var tasks = manager.getStoppedTasks();
        assertTrue(tasks.size() == 1);
    }

    @Test
    public void stopTaskValid() {
        TaskManager manager = TaskManager.getInstance(jda, TASK_PACKAGE);
        boolean result = manager.stopTask("SimpleTask");
        manager.startTask("SimpleTask"); // cleanup
        assertTrue(result);
    }

    @Test
    public void startTaskValid() {
        TaskManager manager = TaskManager.getInstance(jda, TASK_PACKAGE);
        boolean result = manager.startTask("SimpleDisabledTask");
        manager.stopTask("SimpleDisabledTask"); // clean up
        assertTrue(result);
    }

    @Test
    public void startTaskInvalid() {
        TaskManager manager = TaskManager.getInstance(jda, TASK_PACKAGE);
        boolean result = manager.startTask("SimpleTask");
        assertFalse(result);
    }

}
