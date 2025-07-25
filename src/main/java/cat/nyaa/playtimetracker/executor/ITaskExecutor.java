package cat.nyaa.playtimetracker.executor;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface ITaskExecutor {

    /**
     * Executes a task in the sync thread (game loop main thread).
     * Execute order is not guaranteed.
     * @param task the task to be executed
     */
    void sync(ITask task);

    /**
     * Executes a task in a async thread (not game loop main thread).
     * Execute order is not guaranteed.
     * @param task the task to be executed
     */
    void async(ITask task);

    /**
     * Schedules a task to be executed after a specified delay.
     *
     * @param task the task to be scheduled
     * @param delay the delay before execution
     * @param unit the time unit of the delay
     * @return a handle for the scheduled task
     */
    Object scheduleAsync(ITask task, long delay, TimeUnit unit);

    /**
     * Cancels a task.
     *
     * @param handle the handle of the scheduled or event task to be cancelled
     * @return true if the task was successfully cancelled, false otherwise
     */
    boolean cancelTask(Object handle);
}
