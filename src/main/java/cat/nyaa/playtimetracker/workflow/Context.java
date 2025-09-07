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

public class Context {

    protected final Plugin plugin;
    protected final ITaskExecutor executor;
    protected final MissionConfig missionConfig;
    protected final DatabaseManager databaseManager;

    public Context(Plugin plugin, ITaskExecutor executor, MissionConfig missionConfig, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.executor = executor;
        this.missionConfig = missionConfig;
        this.databaseManager = databaseManager;
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
}
