package uk.co.markg.paperclip.task.tasks;

import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import uk.co.markg.paperclip.task.Task;

@Task(unit = TimeUnit.SECONDS, frequency = 10, disabled = true)
public class SimpleDisabledTask implements Runnable {

    private JDA jda;

    public SimpleDisabledTask(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

}
