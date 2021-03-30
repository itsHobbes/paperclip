package uk.co.markg.paperclip.task;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dv8tion.jda.api.JDA;

public class TaskManager {

    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
    private static final int INIT_POOL_SIZE = 1;

    private static volatile TaskManager instance;
    private JDA jda;
    private final Map<Class<? extends Runnable>, ScheduledFuture<?>> runningTasks;
    private final ScheduledExecutorService scheduler;
    private final Set<Class<? extends Runnable>> disabledTasks;

    private TaskManager(JDA jda, String taskPackage) {
        this.jda = jda;
        this.runningTasks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(INIT_POOL_SIZE);
        this.disabledTasks = Collections.synchronizedSet(new HashSet<>());
        registerTasks(taskPackage);
    }

    private void registerTasks(String taskPackage) {
        var reflections = new Reflections(taskPackage);
        Set<Class<? extends Runnable>> classes = reflections.getSubTypesOf(Runnable.class);
        for (var taskClass : classes) {
            registerTask(taskClass);
        }
    }

    private void registerTask(Class<? extends Runnable> taskClass) {
        if (!taskClass.isAnnotationPresent(Task.class)) {
            logger.warn(
                    "Class {} does not have the @Task annotation. This can be ignored if it is not meant to be a task class.",
                    taskClass.getName());
            return;
        }
        Task taskDetails = taskClass.getAnnotation(Task.class);
        if (taskDetails.disabled()) {
            disabledTasks.add(taskClass);
            return;
        }
        startTask(taskClass);
    }

    private void addTask(Object task, long initialDelay, Task taskDetails,
            Class<? extends Runnable> taskClass) {
        var future = scheduler.scheduleAtFixedRate((Runnable) task, initialDelay,
                taskDetails.frequency(), taskDetails.unit());
        runningTasks.put(taskClass, future);
        if (disabledTasks.contains(taskClass)) {
            disabledTasks.remove(taskClass);
        }
        logger.info("Registered {} Task with delay {}", taskClass.getSimpleName(), initialDelay);
    }

    public static TaskManager getInstance(JDA jda, String taskPackage) {
        if (instance == null) {
            synchronized (TaskManager.class) {
                if (instance == null) {
                    instance = new TaskManager(jda, taskPackage);
                }
            }
        }
        return instance;
    }

    public boolean startTask(String taskName) {
        return findStoppedTask(taskName).map(task -> startTask(task)).orElse(false);
    }

    private boolean startTask(Class<? extends Runnable> taskClass) {
        try {
            Task taskDetails = taskClass.getAnnotation(Task.class);
            long initialDelay = 0;
            Object taskObject = taskClass.getConstructor(JDA.class).newInstance(jda);
            if (DelayedTask.class.isAssignableFrom(taskClass)) {
                Method getDelay = taskClass.getMethod("getTaskDelay");
                initialDelay = (long) getDelay.invoke(taskObject);
            }
            addTask(taskObject, initialDelay, taskDetails, taskClass);
            return true;
        } catch (ReflectiveOperationException e) {
            logger.error("Error starting tasks", e);
        }
        return false;
    }

    public Set<Class<? extends Runnable>> getRunningTasks() {
        return runningTasks.keySet();
    }

    public Set<Class<? extends Runnable>> getStoppedTasks() {
        return disabledTasks;
    }

    public boolean stopTask(String taskName) {
        return findRunningTask(taskName).map(task -> cancelTask(task)).orElse(false);
    }

    private boolean cancelTask(Class<? extends Runnable> task) {
        var future = runningTasks.get(task);
        if (future != null) {
            disabledTasks.add(task);
            future.cancel(false);
            runningTasks.remove(task);
            logger.info("Task {} stopped", task.getName());
        }
        return true;
    }

    private Optional<Class<? extends Runnable>> findStoppedTask(String taskName) {
        return disabledTasks.stream().filter(task -> task.getSimpleName().equals(taskName))
                .findFirst();
    }

    private Optional<Class<? extends Runnable>> findRunningTask(String taskName) {
        return runningTasks.keySet().stream().filter(task -> task.getSimpleName().equals(taskName))
                .findFirst();
    }
}
