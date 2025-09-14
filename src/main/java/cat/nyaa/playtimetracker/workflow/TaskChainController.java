package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.executor.ITaskExecutor;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public abstract class TaskChainController implements AutoCloseable {

    public static final Logger logger = LoggerUtils.getPluginLogger();

    private volatile boolean running;
    private final Map<UUID, Chain> playerTasks;
    private final Duration maxWaitTime;

    public TaskChainController(Duration maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
        this.running = true;
        this.playerTasks = new Object2ObjectOpenHashMap<>();
    }

    public abstract ITaskExecutor getExecutor();

    public void scheduleWorkflowTask(UUID playerUUID, Duration delay, ITask task, boolean sync, boolean removeIfEmpty) {
        if (!delay.isPositive() || delay.compareTo(this.maxWaitTime) > 0) {
            logger.warn("Controller.scheduleWorkflowTask called with invalid delay: {} for player={}; skip", delay, playerUUID);
            return;
        }
        var timerPrecision = this.getExecutor().getTimerPrecision();
        TimeUnit unit = timerPrecision.right();
        var delayValue = unit.convert(delay) + timerPrecision.leftLong();
        var handle = sync ?
                this.getExecutor().scheduleSync(task, delayValue, unit) :
                this.getExecutor().scheduleAsync(task, delayValue, unit);
        if (handle != null) {
            Object legacyHandle;
            synchronized (this.playerTasks) {
                var chain = this.playerTasks.computeIfAbsent(playerUUID, Chain::new);
                legacyHandle = chain.waitingHandle;
                chain.waitingHandle = handle;
                chain.removeIfEmpty = removeIfEmpty;
            }
            if (legacyHandle != null) {
                var success = this.getExecutor().cancelTask(legacyHandle);
                logger.debug("Controller.scheduleWorkflowTask cancelled previous task for player={}: success={}", playerUUID, success);
            }
        }
        if (!this.running) {
            cancelScheduledWorkflowTask(playerUUID);
        }
    }

    public void cancelScheduledWorkflowTask(UUID playerUUID) {
        Object legacyHandle = null;
        boolean removed = false;
        synchronized (this.playerTasks) {
            var chain = this.playerTasks.get(playerUUID);
            if (chain != null) {
                legacyHandle = chain.waitingHandle;
                chain.waitingHandle = null;
                if (chain.removeIfEmpty && !chain.running) {
                    this.playerTasks.remove(playerUUID);
                    removed = true;
                }
            }
        }
        if (removed) {
            logger.debug("Controller.cancelScheduledWorkflowTask removed empty chain for player={}", playerUUID);
        }
        if (legacyHandle != null) {
            var success = this.getExecutor().cancelTask(legacyHandle);
            logger.debug("Controller.cancelScheduledWorkflowTask cancelled previous task for player={}: success={}", playerUUID, success);
        }
    }

    public void pushWorkflowTask(UUID playerUUID, ITask task, boolean removeIfEmpty) {
        Object legacyHandle;
        Node toRun = null;
        synchronized (this.playerTasks) {
            var chain = this.playerTasks.computeIfAbsent(playerUUID, Chain::new);
            legacyHandle = chain.waitingHandle;
            chain.waitingHandle = null;
            chain.removeIfEmpty = removeIfEmpty;
            chain.push(task);
            if (!chain.running) {
                toRun = chain.poll();
                chain.running = toRun != null;
            }
        }
        if (legacyHandle != null) {
            var success = this.getExecutor().cancelTask(legacyHandle);
            logger.debug("Controller.pushWorkflowTask cancelled previous task for player={}: success={}", playerUUID, success);
        }
        if (toRun != null) {
            this.getExecutor().async(toRun.task);
        }
    }

    public int pushWorkflowTaskBatch(UUID[] playerUUIDs, ITask[] tasks, int size) {
        if (size <= 0 || playerUUIDs.length < size || tasks.length < size) {
            throw new IllegalArgumentException("Invalid arguments for pushWorkflowTaskBatch");
        }
        Node[] toRuns = new Node[size];
        Object[] legacyHandles = new Object[size];
        synchronized (this.playerTasks) {
            for (int i = 0; i < size; i++) {
                var playerUUID = playerUUIDs[i];
                var task = tasks[i];
                var chain = this.playerTasks.computeIfAbsent(playerUUID, Chain::new);
                legacyHandles[i] = chain.waitingHandle;
                chain.waitingHandle = null;
                chain.push(task);
                if (!chain.running) {
                    var toRun = chain.poll();
                    chain.running = toRun != null;
                    toRuns[i] = toRun;
                }
            }
        }
        int count = 0;
        for (int i = 0; i < size; i++) {
            var legacyHandle = legacyHandles[i];
            if (legacyHandle != null) {
                var success = this.getExecutor().cancelTask(legacyHandle);
                logger.debug("Controller.pushWorkflowTaskBatch cancelled previous task for player={}: success={}", playerUUIDs[i], success);
            }
            var toRun = toRuns[i];
            if (toRun != null) {
                this.getExecutor().async(toRun.task);
                count++;
            }
        }
        return count;
    }

    public void triggerNextWorkflowTask(UUID playerUUID) {
        Node toRun = null;
        boolean removed = false;
        synchronized (this.playerTasks) {
            var chain = this.playerTasks.get(playerUUID);
            if (chain != null) {
                toRun = chain.poll();
                if (toRun == null) {
                    chain.running = false;
                    if (chain.removeIfEmpty && chain.waitingHandle == null) {
                        this.playerTasks.remove(playerUUID);
                        removed = true;
                    }
                }
            }
        }
        if (toRun != null) {
            this.getExecutor().async(toRun.task);
        } else if (removed) {
            logger.debug("Controller.triggerNextWorkflowTask removed empty chain for player={}", playerUUID);
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void close() throws Exception {
        this.running = false;
        OnClose onClose;
        synchronized (this.playerTasks) {
            onClose = new OnClose(this.playerTasks.size());
            this.playerTasks.forEach(onClose);
        }
        onClose.cancelScheduledTasks(this.getExecutor());
    }

    private static class Node {
        final ITask task;
        Node next;

        Node(ITask task) {
            this.task = task;
        }
    }

    private static class Chain extends Node {

        final UUID key;
        Node tail;
        Object waitingHandle;
        boolean running;
        boolean removeIfEmpty;

        Chain(UUID key) {
            super(null);
            this.key = key;
            this.tail = this;
            this.waitingHandle = null;
            this.running = false;
            this.removeIfEmpty = false;
        }

        Node push(ITask task) {
            var node = new Node(task);
            this.tail.next = node;
            this.tail = node;
            return node;
        }

        @Nullable Node poll() {
            var head = this.next;
            if (head == null) {
                return null;
            }
            this.next = head.next;
            if (this.next == null) {
                this.tail = this;
            }
            return head;
        }
    }

    private static class OnClose implements BiConsumer<UUID, Chain> {

        Object[] handles;
        int index;

        OnClose(int capacity) {
            this.handles = new Object[capacity];
            this.index = 0;
        }

        @Override
        public void accept(UUID uuid, Chain chain) {
            this.handles[this.index++] = chain.waitingHandle;
            chain.waitingHandle = null;
            chain.running = false;
            chain.removeIfEmpty = true;
        }

        void cancelScheduledTasks(ITaskExecutor executor) {
            for (int i = 0; i < this.index; i++) {
                var handle = this.handles[i];
                if (handle != null) {
                    executor.cancelTask(handle);
                }
            }
        }
    }
}
