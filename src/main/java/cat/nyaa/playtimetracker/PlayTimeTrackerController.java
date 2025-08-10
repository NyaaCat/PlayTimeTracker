package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.MissionConfig;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.executor.ITaskExecutor;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import cat.nyaa.playtimetracker.workflow.*;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class PlayTimeTrackerController extends Context {

    private final PiecewiseTimeInfo.Builder timeBuilder;
    private final Predicate<UUID> playerAfkState;
    private final RepeatedlyTask loopTask;

    public PlayTimeTrackerController(Plugin plugin, ITaskExecutor executor, MissionConfig missionConfig, DatabaseManager databaseManager, PiecewiseTimeInfo.Builder timeBuilder, Predicate<UUID> playerAfkState) {
        super(plugin, executor, missionConfig, databaseManager);
        this.timeBuilder = timeBuilder;
        this.playerAfkState = playerAfkState;
        this.loopTask = new RepeatedlyTask(this.getExecutor(), this::doLoopUpdate, this.timeBuilder);
    }

    public void login(Player player) {
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var playerContext = new PlayerContext(player, this.getPlugin());
        var ops = UpdateTimeRecordTask.OPTION_WAIT_NEXT;
        var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, ops, this::doLoginListReward);
        this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
        logger.info("PlayTimeTrackerController trigger login player={} ops={}", playerContext.getUUID(), ops);
    }

    public void logout(Player player) {
        var playerContext = new PlayerContext(player, this.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var ops = UpdateTimeRecordTask.OPTION_LAST_SEEN;
        if (!this.playerAfkState.test(playerContext.getUUID())) {
            ops |= UpdateTimeRecordTask.OPTION_ACCUMULATE;
        }
        var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, ops);
        this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
        logger.info("PlayTimeTrackerController trigger logout player={} ops={}", playerContext.getUUID(), ops);
    }

    public void awayFromKeyboard(Player player) {
        var playerContext = new PlayerContext(player, this.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var ops = UpdateTimeRecordTask.OPTION_ACCUMULATE;
        var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, ops);
        this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
        logger.info("PlayTimeTrackerController trigger awayFromKeyboard player={} ops={}", playerContext.getUUID(), ops);
    }

    public void backToKeyboard(Player player) {
        var playerContext = new PlayerContext(player, this.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var ops = UpdateTimeRecordTask.OPTION_WAIT_NEXT;
        var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, ops);
        this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
        logger.info("PlayTimeTrackerController trigger backToKeyboard player={} ops={}", playerContext.getUUID(), ops);
    }

    public void update(Player player) {
        var playerContext = new PlayerContext(player, this.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var ops = UpdateTimeRecordTask.OPTION_NONE;
        if (!this.playerAfkState.test(player.getUniqueId())) {
            ops |= (UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_WAIT_NEXT);
        }
        var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, ops);
        this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
        logger.info("PlayTimeTrackerController trigger update player={} ops={}", playerContext.getUUID(), ops);
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
            if (this.playerAfkState.test(player.getUniqueId())) {
                continue; // skip AFK players
            }
            var playerContext = new PlayerContext(player, this.getPlugin());
            var ops = UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_WAIT_NEXT;
            var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, ops);
            this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
            logger.info("PlayTimeTrackerController trigger(batch) update player={} ops={}", playerContext.getUUID(), ops);
            count++;
        }
        return count;
    }

    public void viewOnlinePlayTime(Player player, CommandSender sender, DisplayNextMissionMode mode) {
        var playerContext = new PlayerContext(player, this.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        final var targetName = player.getName();
        IPostCheckCallback callback = (PlayerContext _playerContext, TimeTrackerDbModel model, List<Pair<String, Duration>> records) -> {
            if (!this.isRunning()) {
                return;
            }
            var displayPlayTimeTask = new DisplayPlayTimeTask(targetName, model, records, sender, mode);
            this.getExecutor().sync(displayPlayTimeTask);
        };
        var ops = UpdateTimeRecordTask.OPTION_NONE;
        if (!this.playerAfkState.test(playerContext.getUUID())) {
            ops |= (UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_WAIT_NEXT);
        }
        var workflowTask = new UpdateTimeRecordTask(this, playerContext, time, ops, callback);
        this.triggerWorkflowTask0(playerContext.getUUID(), workflowTask);
        logger.info("PlayTimeTrackerController trigger showPlayTime online player={} ops={}", playerContext.getUUID(), ops);
    }

    public void viewOfflinePlayTime(UUID targetUUID, String targetName, CommandSender sender) {
        var displayPlayTimeTask = new DisplayPlayTimeTask(this, targetUUID, targetName, sender);
        this.getExecutor().async(displayPlayTimeTask);
        logger.info("PlayTimeTrackerController trigger showPlayTime player={}", targetUUID);
    }

    public void listReward(Player player, @Nullable String mission) {
        var playerContext = new PlayerContext(player, this.getPlugin());
        var listRewardTask = new ListRewardTask(this, playerContext, mission, true);
        this.getExecutor().async(listRewardTask);
        logger.info("PlayTimeTrackerController trigger list reward player={} mission={}", playerContext.getUUID(), mission);
    }

    public void acquireReward(Player player, @Nullable String mission) {
        var playerContext = new PlayerContext(player, this.getPlugin());
        var acquireRewardTask = new AcquireRewardTask(this, playerContext, mission);
        this.getExecutor().async(acquireRewardTask);
        logger.info("PlayTimeTrackerController trigger acquire reward player={} mission={}", playerContext.getUUID(), mission);
    }

    public void resetTimeForOnlinePlayer(Player player, CommandSender sender) {
        var resetTask = new ResetPlayerTimeTask(this, sender, player.getUniqueId(), player.getName(), (@Nullable Long tick) -> this.update(player), null);
        this.getExecutor().async(resetTask);
        logger.info("PlayTimeTrackerController trigger reset time for online player={}", player.getUniqueId());
    }

    public void resetTimeForOfflinePlayer(UUID targetUUID, String targetName, CommandSender sender) {
        var resetTask = new ResetPlayerTimeTask(this, sender, targetUUID, targetName, null, null);
        this.getExecutor().async(resetTask);
        logger.info("PlayTimeTrackerController trigger reset time for offline player={}", targetUUID);
    }

    public void resetMissionForOnlinePlayer(Player player, CommandSender sender, @Nullable String missionName) {
        var resetTask = new ResetPlayerMissionTask(this, sender, player.getUniqueId(), player.getName(), missionName, (@Nullable Long tick) -> this.update(player), null);
        this.getExecutor().async(resetTask);
        logger.info("PlayTimeTrackerController trigger reset mission for online player={} mission={}", player.getUniqueId(), missionName);
    }

    public void resetMissionForOfflinePlayer(UUID targetUUID, String targetName, CommandSender sender, @Nullable String missionName) {
        var resetTask = new ResetPlayerMissionTask(this, sender, targetUUID, targetName, missionName, null, null);
        this.getExecutor().async(resetTask);
        logger.info("PlayTimeTrackerController trigger reset mission for offline player={} mission={}", targetUUID, missionName);
    }

    @Override
    public void close() throws Exception {
        this.loopTask.cancel();
        super.close();
    }

    private void doLoopUpdate(@Nullable Long tick) {
        if (!this.isRunning()) {
            return;
        }
        if (tick == null) {
            this.getExecutor().sync(this::doLoopUpdate);
        } else {
            int count = this.updateAll();
            logger.info("PlayTimeTrackerController loop update executed, updated {} players", count);
        }
    }

    private void doLoginListReward(final PlayerContext playerContext, final TimeTrackerDbModel model, final List<Pair<String, Duration>> records) {
        var listRewardTask = new ListRewardTask(this, playerContext, null, false);
        this.getExecutor().sync(listRewardTask); // TODO
    }
}
