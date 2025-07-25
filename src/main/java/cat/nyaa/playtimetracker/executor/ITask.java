package cat.nyaa.playtimetracker.executor;

import org.jetbrains.annotations.Nullable;

public interface ITask {

    /**
     * Execute the task once time
     * @param tick the current tick for sync task, or null for async task
     */
    void execute(@Nullable Long tick);

}
