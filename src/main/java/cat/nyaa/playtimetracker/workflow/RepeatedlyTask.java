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
    private final ITask callback;
    private final PiecewiseTimeInfo time;
    private Object handle;

    public RepeatedlyTask(ITaskExecutor executor, ITask callback, PiecewiseTimeInfo.Builder timeBuilder) {
        this.executor = executor;
        this.callback = callback;
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
        var currentTimestamp = this.time.getTimestamp();
        var isNextDay = nextDayStart <= currentTimestamp;
        var isNextWeek = nextWeekStart <= currentTimestamp;
        var isNextMonth = nextMonthStart <= currentTimestamp;
        logger.trace("RepeatedlyTask execute current time: {}, next day start: {}, next week start: {}, next month start: {}", currentTime, isNextDay, isNextWeek, isNextMonth);
        nextWait();
        if (isNextDay || isNextWeek || isNextMonth) {
            this.callback.execute(tick);
        }
    }

    private void nextWait() {
        var wait = Long.MAX_VALUE;
        var waitDay = this.time.getNextDayStart() - this.time.getTimestamp();
        if (waitDay > 0 && waitDay < wait) {
            wait = waitDay;
        }
        var waitWeek = this.time.getNextWeekStart() - this.time.getTimestamp();
        if (waitWeek > 0 && waitWeek < wait) {
            wait = waitWeek;
        }
        var waitMonth = this.time.getNextMonthStart() - this.time.getTimestamp();
        if (waitMonth > 0 && waitMonth < wait) {
            wait = waitMonth;
        }
        var timerPrecision = this.executor.getTimerPrecision();
        var offset = timerPrecision.right().toMillis(timerPrecision.leftLong());
        wait += offset;
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
}
