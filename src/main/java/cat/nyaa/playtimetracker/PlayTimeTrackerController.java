package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.MissionConfig;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.executor.ITaskExecutor;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import cat.nyaa.playtimetracker.workflow.*;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class PlayTimeTrackerController extends Context {

    private final PiecewiseTimeInfo.Builder timeBuilder;
    private final RepeatedlyTask loopTask;

    public PlayTimeTrackerController(Plugin plugin, ITaskExecutor executor, MissionConfig missionConfig, DatabaseManager databaseManager, PiecewiseTimeInfo.Builder timeBuilder) {
        super(plugin, executor, missionConfig, databaseManager);
        this.timeBuilder = timeBuilder;
        var callbacks = new RepeatedlyTask.Callbacks();
        callbacks.onDayStartSync = this::updateAllSync;
        this.loopTask = new RepeatedlyTask(this.executor, callbacks, this.timeBuilder);
    }

    public boolean login(Player player) {
        if (!this.isRunning()) {
            return false;
        }
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var playerContext = new PlayerContext(player, this.getPlugin());
        var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, UpdateTimeRecordTask.OPTION_WAIT_NEXT, this::doLoginListReward);
        this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
        logger.info("PlayTimeTrackerController trigger login player={}", playerContext.getUUID());
        return true;
    }

    public boolean logout(Player player) {
        if (!this.isRunning()) {
            return false;
        }
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var playerContext = new PlayerContext(player, this.getPlugin());
        var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_LAST_SEEN);
        this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
        logger.info("PlayTimeTrackerController trigger logout player={}", playerContext.getUUID());
        return true;
    }

    public boolean update(Player player) {
        if (!this.isRunning()) {
            return false;
        }
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var playerContext = new PlayerContext(player, this.getPlugin());
        var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_WAIT_NEXT);
        this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
        logger.info("PlayTimeTrackerController trigger update player={}", playerContext.getUUID());
        return true;
    }

    public int updateAll() {
        int count = 0;
        if (!this.isRunning()) {
            return count;
        }
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var players = this.getPlugin().getServer().getOnlinePlayers();
        for (var player : players) {
            var playerContext = new PlayerContext(player, this.getPlugin());
            var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_WAIT_NEXT);
            this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
            logger.info("PlayTimeTrackerController trigger(batch) update player={}", playerContext.getUUID());
            count++;
        }
        return count;
    }

    public boolean viewPlayTime(OfflinePlayer player, CommandSender sender) {
        if (!this.isRunning()) {
            return false;
        }
        if (player.isOnline()) {
            var currentTime = TimeUtils.getInstantNow();
            var time = this.timeBuilder.build(currentTime);
            var playerContext = new PlayerContext(player, this.getPlugin());
            var callback = new DisplayPlayTimeCallback(player.getName(), sender);
            var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_WAIT_NEXT, callback);
            this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
            logger.info("PlayTimeTrackerController trigger showPlayTime online player={}", playerContext.getUUID());
        } else {
            var displayPlayTimeTask = new DisplayPlayTimeTask(this, player.getUniqueId(), player.getName(), sender);
            this.getExecutor().async(displayPlayTimeTask);
            logger.info("PlayTimeTrackerController trigger showPlayTime offline player={}", player.getUniqueId());
        }
        return true;
    }

    public boolean viewPlayTime(UUID targetUUID, String targetName, CommandSender sender) {
        if (!this.isRunning()) {
            return false;
        }
        var displayPlayTimeTask = new DisplayPlayTimeTask(this, targetUUID, targetName, sender);
        this.getExecutor().async(displayPlayTimeTask);
        logger.info("PlayTimeTrackerController trigger showPlayTime player={}", targetUUID);
        return true;
    }

    public boolean listReward(Player player, @Nullable String mission) {
        if (!this.isRunning()) {
            return false;
        }
        var playerContext = new PlayerContext(player, this.getPlugin());
        var listRewardTask = new ListRewardTask(this, playerContext, mission, true);
        this.getExecutor().async(listRewardTask);
        logger.info("PlayTimeTrackerController trigger list reward player={} mission={}", playerContext.getUUID(), mission);
        return true;
    }

    public boolean acquireReward(Player player, @Nullable String mission) {
        if (!this.isRunning()) {
            return false;
        }
        var playerContext = new PlayerContext(player, this.getPlugin());
        var acquireRewardTask = new AcquireRewardTask(this, playerContext, mission);
        this.getExecutor().async(acquireRewardTask);
        logger.info("PlayTimeTrackerController trigger acquire reward player={} mission={}", playerContext.getUUID(), mission);
        return true;
    }

    private void updateAllSync(Long tick) {
        updateAll();
    }

    @Override
    public void close() throws Exception {
        this.loopTask.cancel();
        super.close();
    }

    private void doLoginListReward(final PlayerContext playerContext, final TimeTrackerDbModel model, final List<Pair<String, Duration>> records) {
        if (!this.isRunning()) {
            return;
        }
        var listRewardTask = new ListRewardTask(this, playerContext, null, false);
        this.getExecutor().async(listRewardTask);
    }

    private class DisplayPlayTimeCallback implements IPostCheckCallback {
        private final String targetName;
        private final CommandSender sender;

        public DisplayPlayTimeCallback(String targetName, CommandSender sender) {
            this.targetName = targetName;
            this.sender = sender;
        }

        @Override
        public void handle(PlayerContext playerContext, TimeTrackerDbModel model, List<Pair<String, Duration>> records) {
            if (!PlayTimeTrackerController.this.isRunning()) {
                return;
            }
            var displayPlayTimeTask = new DisplayPlayTimeTask(targetName, model, sender);
            PlayTimeTrackerController.this.getExecutor().sync(displayPlayTimeTask);
        }
    }
}
