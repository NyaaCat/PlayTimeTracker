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
        logger.info("RepeatedlyTask start");
        nextWait();
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
        nextWait();
    }

    private void nextWait() {
        var wait = this.time.getNextDayStart() - this.time.getTimestamp();
        this.handle = this.executor.scheduleAsync(this, wait, TimeUnit.MILLISECONDS);
        logger.info("RepeatedlyTask next wait for {}ms", wait);
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

    public static class Callbacks {
        @Nullable public ITask onDayStartAsync;
        @Nullable public ITask onWeekStartAsync;
        @Nullable public ITask onMonthStartAsync;
        @Nullable public ITask onDayStartSync;
        @Nullable public ITask onWeekStartSync;
        @Nullable public ITask onMonthStartSync;

        public Callbacks() {
        }

        public Callbacks(@Nullable ITask onDayStartAsync,
                         @Nullable ITask onWeekStartAsync,
                         @Nullable ITask onMonthStartAsync,
                         @Nullable ITask onDayStartSync,
                         @Nullable ITask onWeekStartSync,
                         @Nullable ITask onMonthStartSync) {
            this.onDayStartAsync = onDayStartAsync;
            this.onWeekStartAsync = onWeekStartAsync;
            this.onMonthStartAsync = onMonthStartAsync;
            this.onDayStartSync = onDayStartSync;
            this.onWeekStartSync = onWeekStartSync;
            this.onMonthStartSync = onMonthStartSync;
        }
    }
}
