package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.executor.ITaskExecutor;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public class RepeatedlyTask implements ITask {

    public final static Logger logger = LoggerUtils.getPluginLogger();

    protected final ITaskExecutor executor;
    private final Callbacks callbacks;
    private final PiecewiseTimeInfo time;
    private Object handle;

    public RepeatedlyTask(ITaskExecutor executor, Callbacks callbacks, PiecewiseTimeInfo.Builder timeBuilder) {
        this.executor = executor;
        this.callbacks = callbacks;
        var currentTime = TimeUtils.getInstantNow();
        this.time = timeBuilder.build(currentTime);
        var wait = this.time.getNextDayStart() - this.time.getTimestamp();
        this.handle = this.executor.scheduleAsync(this, wait, TimeUnit.MILLISECONDS);
        logger.info("RepeatedlyTask start");
    }

    @Override
    public void execute(@Nullable Long tick) {
        var nextDayStart = this.time.getNextDayStart();
        var nextWeekStart = this.time.getNextWeekStart();
        var nextMonthStart = this.time.getNextMonthStart();
        var currentTime = TimeUtils.getInstantNow();
        this.time.updateTime(currentTime);
        if (nextDayStart <= this.time.getTimestamp()) {
            if (this.callbacks.onDayStartAsync != null) {
                try {
                    this.callbacks.onDayStartAsync.execute(null);
                } catch (Exception e) {
                    logger.error("RepeatedlyTask error running next day task", e);
                }
            }
            if (this.callbacks.onDayStartSync != null) {
                this.executor.sync(this.callbacks.onDayStartSync);
            }
        }
        if (nextWeekStart <= this.time.getTimestamp()) {
            if (this.callbacks.onWeekStartAsync != null) {
                try {
                    this.callbacks.onWeekStartAsync.execute(null);
                } catch (Exception e) {
                    logger.error("RepeatedlyTask error running next week task", e);
                }
            }
            if (this.callbacks.onWeekStartSync != null) {
                this.executor.sync(this.callbacks.onWeekStartSync);
            }
        }
        if (nextMonthStart <= this.time.getTimestamp()) {
            if (this.callbacks.onMonthStartAsync != null) {
                try {
                    this.callbacks.onMonthStartAsync.execute(null);
                } catch (Exception e) {
                    logger.error("RepeatedlyTask error running next month task", e);
                }
            }
            if (this.callbacks.onMonthStartSync != null) {
                this.executor.sync(this.callbacks.onMonthStartSync);
            }
        }
        var wait = this.time.getNextDayStart() - this.time.getTimestamp();
        this.handle = this.executor.scheduleAsync(this, wait, TimeUnit.MILLISECONDS);
    }

    public boolean cancel() {
        if (this.handle != null) {
            boolean result = this.executor.cancelTask(this.handle);
            if (result) {
                this.handle = null;
                logger.info("RepeatedlyTask stop");
            }
            return result;
        }
        return false;
    }

    public record Callbacks(@Nullable ITask onDayStartAsync,
                            @Nullable ITask onWeekStartAsync,
                            @Nullable ITask onMonthStartAsync,
                            @Nullable ITask onDayStartSync,
                            @Nullable ITask onWeekStartSync,
                            @Nullable ITask onMonthStartSync) {

    }
}
