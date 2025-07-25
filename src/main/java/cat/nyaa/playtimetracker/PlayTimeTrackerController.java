package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.MissionConfig;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.executor.ITaskExecutor;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import cat.nyaa.playtimetracker.workflow.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayTimeTrackerController extends Context {

    private final PiecewiseTimeInfo.Builder timeBuilder;

    public PlayTimeTrackerController(Plugin plugin, ITaskExecutor executor, MissionConfig missionConfig, DatabaseManager databaseManager, PiecewiseTimeInfo.Builder timeBuilder) {
        super(plugin, executor, missionConfig, databaseManager);
        this.timeBuilder = timeBuilder;
    }

    public void login(UUID playerUUID) {
        if (!this.isRunning()) {
            return;
        }
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var playerContext = this.buildPlayerContext(playerUUID);
        this.triggerWorkflowTask0(playerContext, time, TriggerEvent.LOGIN, null);
        this.triggerListReward0(playerContext, null);
    }

    public void logout(UUID playerUUID) {
        if (!this.isRunning()) {
            return;
        }
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var playerContext = this.buildPlayerContext(playerUUID);

    }

    public void update(UUID playerUUID) {
        if (!this.isRunning()) {
            return;
        }
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var playerContext = this.buildPlayerContext(playerUUID);
        this.triggerWorkflowTask0(playerContext, time, TriggerEvent.UPDATE, null);
    }

    public void updateAll() {
        if (!this.isRunning()) {
            return;
        }
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var players = this.getPlugin().getServer().getOnlinePlayers();
        for (var player : players) {
            var playerContext = this.buildPlayerContext(player.getUniqueId());
            this.triggerWorkflowTask0(playerContext, time, TriggerEvent.UPDATE, null);
        }
    }

    protected void triggerWorkflowTask0(PlayerContext playerContext, PiecewiseTimeInfo time, TriggerEvent event, @Nullable String mission) {
        var task = new UpdateTimeRecordTask(this, playerContext, time, event, null);
        this.triggerWorkflowTask0(playerContext.getUUID(), task);
        logger.info("PlayTimeTrackerController trigger workflow: event {} for player {} with mission {}", event, playerContext.getUUID(), mission);
    }

    protected void triggerListReward0(PlayerContext playerContext, @Nullable String mission) {
        var listRewardTask = new ListRewardTask(this, playerContext, mission);
        this.getExecutor().async(listRewardTask);
        logger.info("PlayTimeTrackerController trigger list-reward: player {} with mission {}", playerContext.getUUID(), mission);
    }


    public static class PTTListener implements Listener {

        private final PlayTimeTrackerController controller;

        public PTTListener(PlayTimeTrackerController controller) {
            this.controller = controller;
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onPlayerJoin(PlayerJoinEvent event) {
            var player = event.getPlayer();
            this.controller.login(player.getUniqueId());
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onPlayerQuit(PlayerQuitEvent event) {
            var player = event.getPlayer();
            this.controller.logout(player.getUniqueId());
        }
    }
}
