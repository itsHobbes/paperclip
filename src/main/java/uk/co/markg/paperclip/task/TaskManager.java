package uk.co.markg.paperclip.task;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final Map<Class<? extends Runnable>, ScheduledFuture<?>> tasks;
    private final ScheduledExecutorService scheduler;
    private final List<Class<? extends Runnable>> disabledTasks;

    private TaskManager(JDA jda, String taskPackage) {
        this.jda = jda;
        this.tasks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(INIT_POOL_SIZE);
        this.disabledTasks = Collections.synchronizedList(new ArrayList<>());
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
            logger.error("Task {} does not have the @Task annotation", taskClass.getName());
            return;
        }
        Task taskDetails = taskClass.getAnnotation(Task.class);
        if (taskDetails.disabled()) {
            disabledTasks.add(taskClass);
            return;
        }
        startTask(taskClass, taskDetails);
    }

    private void addTask(Object task, long initialDelay, Task taskDetails,
            Class<? extends Runnable> taskClass) {
        var future = scheduler.scheduleAtFixedRate((Runnable) task, initialDelay,
                taskDetails.frequency(), taskDetails.unit());
        tasks.put(taskClass, future);
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

    public void startTask(String taskName) {
        try {
            Class<? extends Runnable> taskClass = findStoppedTask(taskName);
            Task taskDetails = taskClass.getAnnotation(Task.class);
            startTask(taskClass, taskDetails);
        } catch (ClassNotFoundException e) {
            logger.error("Error starting task {}", taskName, e);
        }
    }

    private void startTask(Class<? extends Runnable> taskClass, Task taskDetails) {
        try {
            long initialDelay = 0;
            Object taskObject = taskClass.getConstructor(JDA.class).newInstance(jda);
            if (DelayedTask.class.isAssignableFrom(taskClass)) {
                Method getDelay = taskClass.getMethod("getTaskDelay");
                initialDelay = (long) getDelay.invoke(taskObject);
            }
            addTask(taskObject, initialDelay, taskDetails, taskClass);
        } catch (ReflectiveOperationException e) {
            logger.error("Error starting tasks", e);
        }
    }

    public Set<Class<? extends Runnable>> getTasks() {
        return tasks.keySet();
    }

    public void stopTask(String taskName) {
        try {
            Class<? extends Runnable> taskClass = findRunningTask(taskName);
            var future = tasks.get(taskClass);
            if (future != null) {
                disabledTasks.add(taskClass);
                future.cancel(false);
                tasks.remove(taskClass);
                logger.info("Task {} stopped", taskName);
            }
        } catch (ClassNotFoundException e) {
            logger.error("Error stopping task {}", taskName, e);
        }
    }

    private Class<? extends Runnable> findStoppedTask(String taskName)
            throws ClassNotFoundException {
        return disabledTasks.stream().filter(task -> task.getSimpleName().equals(taskName))
                .findFirst().orElseThrow(ClassNotFoundException::new);
    }

    private Class<? extends Runnable> findRunningTask(String taskName)
            throws ClassNotFoundException {
        return tasks.keySet().stream().filter(task -> task.getSimpleName().equals(taskName))
                .findFirst().orElseThrow(ClassNotFoundException::new);
    }

}
