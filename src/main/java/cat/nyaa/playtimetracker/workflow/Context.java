package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.condition.ICondition;
import cat.nyaa.playtimetracker.config.MissionConfig;
import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.connection.RewardsConnection;
import cat.nyaa.playtimetracker.db.connection.TimeTrackerConnection;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.executor.ITaskExecutor;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Context implements AutoCloseable {

    public static final Logger logger = LoggerUtils.getPluginLogger();

    protected final Plugin plugin;
    protected final ITaskExecutor executor;
    protected final MissionConfig missionConfig;
    protected final DatabaseManager databaseManager;
    private final Map<UUID, Object> playerTasks;
    private final AtomicBoolean running;
    protected Duration maxWaitTime;

    public Context(Plugin plugin, ITaskExecutor executor, MissionConfig missionConfig, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.executor = executor;
        this.missionConfig = missionConfig;
        this.databaseManager = databaseManager;
        this.playerTasks = new ConcurrentHashMap<>();
        this.maxWaitTime = Duration.ofDays(1);
        this.running = new AtomicBoolean(true);
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    public ITaskExecutor getExecutor() {
        return this.executor;
    }

    public MissionConfig getMissionConfig() {
        return this.missionConfig;
    }

    public TimeTrackerConnection getTimeTrackerConnection() {
        return this.databaseManager.getTimeTrackerConnection();
    }

    public CompletedMissionConnection getCompletedMissionConnection() {
        return this.databaseManager.getCompletedMissionConnection();
    }

    public RewardsConnection getRewardsConnection() {
        return this.databaseManager.getRewardsConnection();
    }

    public ICondition<LimitedTimeTrackerModel> buildMissionTimeCondition(MissionData missionData) {
        return (ICondition<LimitedTimeTrackerModel>) missionData.getTimeCondition();
    }

    public boolean scheduleWorkflowTask(Duration delay, UUID playerUUID, ITask task) {
        if (!delay.isPositive() || delay.compareTo(this.maxWaitTime) > 0) {
            logger.warn("Context.scheduleWorkflow called with invalid delay: {} for player={}; skip", delay, playerUUID);
            return false;
        }
        if (this.running.get()) {
            TimeUnit unit = TimeUnit.SECONDS;
            var delayValue = delay.getSeconds() + (delay.getNano() > 0 ? 1L : 0L);
            var handle = this.executor.scheduleAsync(task, delayValue, unit);
            if (handle != null) {
                var legacyHandle = this.playerTasks.put(playerUUID, handle);
                if (legacyHandle != null) {
                    var success = this.executor.cancelTask(legacyHandle);
                    logger.debug("Context.scheduleWorkflow cancelled previous task: success={}", success);
                }
            } else {
                return false;
            }
            if (!this.running.get()) {
                // If the context is not running, we should not keep the task
                var success = this.executor.cancelTask(handle);
                logger.debug("Context.scheduleWorkflow cancelled task due to context not running: success={}", success);
                return false;
            }
            return true;
        } else {
            logger.warn("Context.scheduleWorkflow called when context is not running, task will not be scheduled for player={}", playerUUID);
            return false;
        }
    }

    public boolean triggerWorkflowTask(UUID playerUUID, ITask task) {
        if (this.running.get()) {
            var legacyHandle = this.playerTasks.remove(playerUUID);
            if (legacyHandle != null) {
                var success = this.executor.cancelTask(legacyHandle);
                logger.debug("Context.triggerWorkflowTask cancelled previous task: success={}", success);
            }
            this.executor.async(task);
            return true;
        }
        return false;
    }

    protected void triggerWorkflowTask0(UUID playerUUID, ITask task) {
        var legacyHandle = this.playerTasks.remove(playerUUID);
        if (legacyHandle != null) {
            var success = this.executor.cancelTask(legacyHandle);
            logger.debug("Context.triggerWorkflowTask0 cancelled previous task: success={}", success);
        }
        this.executor.async(task);
    }

    public void removePlayerTask(UUID playerUUID) {
        var handle = this.playerTasks.remove(playerUUID);
        if (handle != null) {
            this.executor.cancelTask(handle);
        }
    }

    public boolean isRunning() {
        return this.running.get();
    }

    @Override
    public void close() throws Exception {
        this.running.set(false);
        this.playerTasks.forEach(this::onCloseRemovePlayerTask);
    }

    private void onCloseRemovePlayerTask(UUID playerUUID, Object handle) {
        this.executor.cancelTask(handle);
    }
}
