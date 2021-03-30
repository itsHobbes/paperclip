package uk.co.markg.paperclip.task.tasks;

import java.util.concurrent.TimeUnit;
import uk.co.markg.paperclip.task.Task;

@Task(unit = TimeUnit.SECONDS, frequency = 10)
public class SimpleNoJDATask implements Runnable {

    @Override
    public void run() {
        // TODO Auto-generated method stub
    }

}
