package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import cat.nyaa.playtimetracker.workflow.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class PlayTimeTrackerController extends TaskChainController {

    private final Context context;
    private final PiecewiseTimeInfo.Builder timeBuilder;
    private final Predicate<UUID> playerAfkState;
    private final RepeatedlyTask loopTask;

    public PlayTimeTrackerController(Context context, PiecewiseTimeInfo.Builder timeBuilder, Predicate<UUID> playerAfkState) {
        super(context.getExecutor(), Duration.ofDays(1));
        this.context = context;
        this.timeBuilder = timeBuilder;
        this.playerAfkState = playerAfkState;
        this.loopTask = new RepeatedlyTask(context.getExecutor(), this::doLoopUpdate, this.timeBuilder);
    }

    public void login(Player player) {
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var ops = UpdateTimeRecordTask.OPTION_CHECK_MISSIONS;
        var workflow = new UpdateWorkflow(playerContext, time, this::onLoginComplete);
        var workflowTask = new UpdateTimeRecordTask(this.context, workflow, ops);
        this.pushWorkflowTask(playerContext.getUUID(), workflowTask, false);
        logger.info("PlayTimeTrackerController trigger login player={} ops={}", playerContext.getUUID(), ops);
    }

    public void logout(Player player) {
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var ops = UpdateTimeRecordTask.OPTION_LAST_SEEN;
        if (!this.playerAfkState.test(playerContext.getUUID())) {
            ops |= UpdateTimeRecordTask.OPTION_ACCUMULATE;
        }
        var workflow = new UpdateWorkflow(playerContext, time, this::onWorkflowComplete);
        var workflowTask = new UpdateTimeRecordTask(this.context, workflow, ops);
        this.pushWorkflowTask(playerContext.getUUID(), workflowTask, true);
        logger.info("PlayTimeTrackerController trigger logout player={} ops={}", playerContext.getUUID(), ops);
    }

    public void awayFromKeyboard(Player player) {
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var ops = UpdateTimeRecordTask.OPTION_ACCUMULATE;
        var workflow = new UpdateWorkflow(playerContext, time, this::onWorkflowComplete);
        var workflowTask = new UpdateTimeRecordTask(this.context, workflow, ops);
        this.pushWorkflowTask(playerContext.getUUID(), workflowTask, false);
        logger.info("PlayTimeTrackerController trigger awayFromKeyboard player={} ops={}", playerContext.getUUID(), ops);
    }

    public void backToKeyboard(Player player) {
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var ops = UpdateTimeRecordTask.OPTION_CHECK_MISSIONS;
        var workflow = new UpdateWorkflow(playerContext, time, this::onWorkflowCompleteWithNext);
        var workflowTask = new UpdateTimeRecordTask(this.context, workflow, ops);
        this.pushWorkflowTask(playerContext.getUUID(), workflowTask, false);
        logger.info("PlayTimeTrackerController trigger backToKeyboard player={} ops={}", playerContext.getUUID(), ops);
    }

    public void update(Player player) {
        if (this.playerAfkState.test(player.getUniqueId())) {
            return;
        }
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var ops = UpdateTimeRecordTask.OPTION_NONE;
        if (!this.playerAfkState.test(player.getUniqueId())) {
            ops |= (UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_CHECK_MISSIONS);
        }
        var workflow = new UpdateWorkflow(playerContext, time, this::onWorkflowCompleteWithNext);
        var workflowTask = new UpdateTimeRecordTask(this.context, workflow, ops);
        this.pushWorkflowTask(playerContext.getUUID(), workflowTask, false);
        logger.info("PlayTimeTrackerController trigger update player={} ops={}", playerContext.getUUID(), ops);
    }

    public int updateAll() {
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        var players = this.context.getPlugin().getServer().getOnlinePlayers();
        UUID[] playerUUIDs = new UUID[players.size()];
        ITask[] tasks = new ITask[players.size()];
        int index = 0;
        for (var player : players) {
            if (this.playerAfkState.test(player.getUniqueId())) {
                continue;
            }
            var playerContext = new PlayerContext(player, this.context.getPlugin());
            var ops = UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_CHECK_MISSIONS;
            var workflow = new UpdateWorkflow(playerContext, time, this::onWorkflowCompleteWithNext);
            var workflowTask = new UpdateTimeRecordTask(this.context, workflow, ops);
            logger.info("PlayTimeTrackerController trigger(batch) update player={} ops={}", playerContext.getUUID(), ops);
            playerUUIDs[index] = playerContext.getUUID();
            tasks[index] = workflowTask;
            index++;
        }
        int executed = this.pushWorkflowTaskBatch(playerUUIDs, tasks, index);
        logger.debug("PlayTimeTrackerController trigger(batch) updateAll count={}, executed={}", index, executed);
        return index;
    }

    public void viewOnlinePlayTime(Player player, CommandSender sender, DisplayNextMissionMode mode) {
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var currentTime = TimeUtils.getInstantNow();
        var time = this.timeBuilder.build(currentTime);
        final var targetName = player.getName();
        BiConsumer<@Nullable Long, UpdateWorkflow> callback = (@Nullable Long tick1, UpdateWorkflow workflow1) -> {
            var model = workflow1.getModel();
            if (model != null) {
                var records = workflow1.getRecordList();
                var displayPlayTimeTask = new DisplayPlayTimeTask(targetName, model, records, sender, mode);
                this.context.getExecutor().sync(displayPlayTimeTask);
            }
            this.onWorkflowCompleteWithNext(tick1, workflow1);
        };
        var ops = UpdateTimeRecordTask.OPTION_NONE;
        if (!this.playerAfkState.test(playerContext.getUUID())) {
            ops |= (UpdateTimeRecordTask.OPTION_ACCUMULATE | UpdateTimeRecordTask.OPTION_CHECK_MISSIONS);
        }
        var workflow = new UpdateWorkflow(playerContext, time, callback);
        var workflowTask = new UpdateTimeRecordTask(this.context, workflow, ops);
        this.pushWorkflowTask(playerContext.getUUID(), workflowTask, false);
        logger.info("PlayTimeTrackerController trigger showPlayTime online player={} ops={}", playerContext.getUUID(), ops);
    }

    public void viewOfflinePlayTime(UUID targetUUID, String targetName, CommandSender sender) {
        var displayPlayTimeTask = new DisplayPlayTimeTask(this.context, targetUUID, targetName, sender);
        this.context.getExecutor().async(displayPlayTimeTask);
        logger.info("PlayTimeTrackerController trigger showPlayTime player={}", targetUUID);
    }

    public void listReward(Player player, @Nullable String mission) {
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var listRewardTask = new ListRewardTask(this.context, playerContext, mission, true);
        this.context.getExecutor().async(listRewardTask);
        logger.info("PlayTimeTrackerController trigger list reward player={} mission={}", playerContext.getUUID(), mission);
    }

    public void acquireReward(Player player, @Nullable String mission) {
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var acquireRewardTask = new AcquireRewardTask(this.context, playerContext, mission);
        this.context.getExecutor().async(acquireRewardTask);
        logger.info("PlayTimeTrackerController trigger acquire reward player={} mission={}", playerContext.getUUID(), mission);
    }

    public void resetTimeForOnlinePlayer(Player player, CommandSender sender) {
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var resetTask = new ResetPlayerTimeTask(this.context, sender, player.getUniqueId(), player.getName(), new ExecuteUpdate(playerContext), null);
        this.context.getExecutor().async(resetTask);
        logger.info("PlayTimeTrackerController trigger reset time for online player={}", player.getUniqueId());
    }

    public void resetTimeForOfflinePlayer(UUID targetUUID, String targetName, CommandSender sender) {
        var resetTask = new ResetPlayerTimeTask(this.context, sender, targetUUID, targetName, null, null);
        this.context.getExecutor().async(resetTask);
        logger.info("PlayTimeTrackerController trigger reset time for offline player={}", targetUUID);
    }

    public void resetMissionForOnlinePlayer(Player player, CommandSender sender, @Nullable String missionName) {
        var playerContext = new PlayerContext(player, this.context.getPlugin());
        var resetTask = new ResetPlayerMissionTask(this.context, sender, player.getUniqueId(), player.getName(), missionName, new ExecuteUpdate(playerContext), null);
        this.context.getExecutor().async(resetTask);
        logger.info("PlayTimeTrackerController trigger reset mission for online player={} mission={}", player.getUniqueId(), missionName);
    }

    public void resetMissionForOfflinePlayer(UUID targetUUID, String targetName, CommandSender sender, @Nullable String missionName) {
        var resetTask = new ResetPlayerMissionTask(this.context, sender, targetUUID, targetName, missionName, null, null);
        this.context.getExecutor().async(resetTask);
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
            this.context.getExecutor().sync(this::doLoopUpdate);
        } else {
            int count = this.updateAll();
            logger.info("PlayTimeTrackerController loop update executed, updated {} players", count);
        }
    }

    private void onWorkflowComplete(@Nullable Long tick, UpdateWorkflow workflow) {
        var uuid = workflow.getPlayerContext().getUUID();
        this.triggerNextWorkflowTask(uuid);
    }

    private void onWorkflowCompleteWithNext(@Nullable Long tick, UpdateWorkflow workflow) {
        var records = workflow.getRecordList();
        if (!records.isEmpty()) {
            var playerContext = workflow.getPlayerContext();
            var pair = records.getFirst();
            this.scheduleWorkflowTask(playerContext.getUUID(), pair.right(), new ExecuteUpdate(playerContext), true, false);
            logger.info("PlayTimeTrackerController schedule next task for player={} next={} in {}", playerContext.getUUID(), pair.left(), pair.right());
        }
        this.onWorkflowComplete(tick, workflow);
    }

    private void onLoginComplete(@Nullable Long tick, UpdateWorkflow workflow) {
        var listRewardTask = new ListRewardTask(this.context, workflow.getPlayerContext(), null, false);
        this.context.getExecutor().async(listRewardTask);
        this.onWorkflowCompleteWithNext(tick, workflow);
    }

    private class ExecuteUpdate implements ITask {

        final PlayerContext playerContext;

        ExecuteUpdate(PlayerContext playerContext) {
            this.playerContext = playerContext;
        }

        @Override
        public void execute(@Nullable Long tick) {
            if (tick == null) {
                // ensure step 0 is executed in sync thread
                PlayTimeTrackerController.this.context.getExecutor().sync(this);
                return;
            }
            var player = playerContext.getPlayer(tick);
            if (player != null) {
                PlayTimeTrackerController.this.update(player);
            }
        }
    }
}
