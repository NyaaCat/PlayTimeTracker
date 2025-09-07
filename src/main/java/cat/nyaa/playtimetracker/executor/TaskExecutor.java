package cat.nyaa.playtimetracker.executor;

import cat.nyaa.playtimetracker.utils.LoggerUtils;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import it.unimi.dsi.fastutil.longs.LongObjectImmutablePair;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TaskExecutor implements ITaskExecutor {

    private static final Logger logger = LoggerUtils.getPluginLogger();
    private final Plugin plugin;
    private final long syncInterval;
    private final LongObjectImmutablePair<TimeUnit> timerPrecision;

    private long syncTick;
    private Executor asyncExecutor;
    private HashedWheelTimer timer;
    private int syncHandle;
    private Queue<ITask> syncTasks;

    public TaskExecutor(Plugin plugin, long syncInterval, long timerInterval, TimeUnit timerIntervalUnit) {
        this.plugin = plugin;
        this.syncInterval = syncInterval;
        this.timerPrecision = LongObjectImmutablePair.of(timerInterval, timerIntervalUnit);

        this.syncTick = 0;
        this.asyncExecutor = null;
        this.timer = null;
        this.syncHandle = -1;

        this.syncTasks = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void sync(ITask task) {
        // TODO: Thread safety
        if (this.syncHandle == -1) {
            return;
        }
        this.syncTasks.offer(task);
    }

    @Override
    public void async(ITask task) {
        // TODO: Thread safety
        var asyncExecutor = this.asyncExecutor;
        if (asyncExecutor == null) {
            return;
        }
        asyncExecutor.execute(() -> {;
            try {
                task.execute(null);
            } catch (Exception e) {
                logger.error("TaskExecutor async task execution failed for task: {}", task, e);
            }
        });
    }

    @Override
    public Object scheduleSync(ITask task, long delay, TimeUnit unit) {
        var timer = this.timer;
        if (timer == null) {
            return null; // Timer not initialized, cannot schedule task
        }
        return timer.newTimeout(new Scheduled(task, true), delay, unit);
    }

    @Override
    public Object scheduleAsync(ITask task, long delay, TimeUnit unit) {
        var timer = this.timer;
        if (timer == null) {
            return null; // Timer not initialized, cannot schedule task
        }
        return timer.newTimeout(new Scheduled(task, false), delay, unit);
    }

    @Override
    public boolean cancelTask(Object handle) {
        if (handle instanceof Timeout timeout) {
            return timeout.cancel();
        }
        return false;
    }

    @Override
    public LongObjectImmutablePair<TimeUnit> getTimerPrecision() {
        return this.timerPrecision;
    }


    private void runSync() {
        var syncTick = this.syncTick++;
        var tasks = this.syncTasks;
        for (var task = tasks.poll(); task != null; task = tasks.poll()) {
            try {
                task.execute(syncTick);
            } catch (Exception e) {
                logger.error("TaskExecutor sync task execution failed for task: {}", task, e);
            }
        }
    }

    public void start() {
        var server = this.plugin.getServer();
        var maxPendingTimeouts = server.getMaxPlayers() + 16;
        var threadFactory = Executors.defaultThreadFactory();
        this.asyncExecutor = Executors.newSingleThreadExecutor(threadFactory);
        this.timer = new HashedWheelTimer(threadFactory, this.timerPrecision.leftLong(), this.timerPrecision.right(), 512, true, maxPendingTimeouts, this.asyncExecutor);
        this.timer.start();
        var scheduler = server.getScheduler();
        this.syncHandle = scheduler.scheduleSyncRepeatingTask(this.plugin, this::runSync, this.syncInterval, this.syncInterval);
        logger.info("TaskExecutor started");
    }

    public void stop() {
        logger.info("TaskExecutor Stopping...");
        if (this.timer != null) {
            this.timer.stop();
            this.timer = null;
        }
        if (this.asyncExecutor != null) {
            this.asyncExecutor = null;
        }
        if (this.syncHandle > -1) {
            var server = this.plugin.getServer();
            var scheduler = server.getScheduler();
            scheduler.cancelTask(this.syncHandle);
            this.syncHandle = -1;
        }
    }

    private class Scheduled implements ITask, TimerTask {

        final ITask inner;
        final boolean sync;
        Timeout timeout;

        Scheduled(ITask inner, boolean sync) {
            this.inner = inner;
            this.sync = sync;
        }

        @Override
        public void execute(@Nullable Long tick) {
            assert this.timeout != null;
            if (this.timeout.isCancelled()) {
                return;
            }
            try {
                this.inner.execute(tick);
            } catch (Exception e) {
                logger.error("TaskExecutor scheduled sync task execution failed for task: {}", this.inner, e);
            }
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled()) {
                return;
            }
            if (this.sync) {
                this.timeout = timeout;
                TaskExecutor.this.syncTasks.add(this);
            } else {
                try {
                    this.inner.execute(null);
                } catch (Exception e) {
                    logger.error("TaskExecutor scheduled async task execution failed for task: {}", this.inner, e);
                }
            }
        }
    }
}
