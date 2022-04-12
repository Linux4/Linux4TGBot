package de.linux4.telegrambot;

import java.util.ArrayList;
import java.util.List;

public class Cron extends Thread {

    private final List<CronTask> tasks = new ArrayList<>();

    @Override
    public void run() {
        List<CronTask> remove = new ArrayList<>();

        for (CronTask task : tasks) {
            if (task.when() <= System.currentTimeMillis()) {
                try {
                    task.task().run();
                    remove.add(task);
                } catch (Exception | Error ex) {
                    ex.printStackTrace();
                }
            }
        }

        tasks.removeAll(remove);
    }

}
