package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import com.earth2me.essentials.User;
import net.ess3.api.IEssentials;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class CheckMissionTask implements ITask {

    private final PlayerRelatedCache playerData;
    private final String missionName;
    private final MissionData missionData;
    private final PiecewiseTimeInfo time;
    private final TimeTrackerDbModel tracker;
    private final IWaitTimeCollection wait;
    private int step;

    public CheckMissionTask(PlayerRelatedCache playerData, String missionName, MissionData missionData, PiecewiseTimeInfo time, TimeTrackerDbModel tracker, IWaitTimeCollection wait) {
        this.playerData = playerData;
        this.missionName = missionName;
        this.missionData = missionData;
        this.time = time;
        this.tracker = tracker;
        this.wait = wait;
        this.step = 0;
    }

    @Override
    public int execute(Workflow workflow, boolean executeInGameLoop) throws Exception {
        switch (this.step) {
            case 0 -> {
                if (!executeInGameLoop) {
                    return -1;
                }
                // step 1: filter in game-loop

                if (!this.checkInGroup(workflow)) {
                    this.step = 0xFF;
                    return 0;
                }

                workflow.addNextWorkStep(this, false);
                this.step = 1;
                return 0;
            }
            case 1 -> {
                if (executeInGameLoop) {
                    return -1;
                }
                // step 2: check asynchronously

                if (!this.checkUncompleted(workflow)) {
                    this.step = 0xFF;
                    return 0;
                }

                Long waitTime = this.checkMissionTimeCondition();
                if (waitTime == null) {
                    // impossible to complete
                    this.step = 0xFF;
                    return 0;
                }

                if (waitTime > 0) {
                    // wait for the time condition
                    this.wait.waitFor(waitTime);
                    this.step = 0xFF;
                    return 0;
                }

                // push reward and notify
                var once = new NotifyRewardsTask.Once();
                for (var e : this.missionData.rewardList.entrySet()) {
                    var rewardTask = new DistributeRewardTask(this.playerData.player,  this.missionName, e.getValue(), this.time, once);
                    if (rewardTask.isRewardValid()) {
                        workflow.addNextWorkStep(rewardTask, true);
                    }
                }

                this.step = 0xFF;
                return 0;
            }
            default -> {
                return -2;
            }
        }
    }

    private boolean checkInGroup(Workflow workflow) {
        if (this.missionData.group != null && !this.missionData.group.isEmpty()) {
            var essUser = this.playerData.getEssUser();
            if (essUser == null) {
                return false;
            }
            for (String group : this.missionData.group) {
                if (!essUser.inGroup(group)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkUncompleted(Workflow workflow) {
        var model = workflow.getCompletedMissionConnection().getPlayerCompletedMission(this.playerData.player.getUniqueId(), this.missionName);
        return model == null;
    }

    // @return the time to wait; None if impossible to complete; 0 if already completed
    private Long checkMissionTimeCondition() {
        var condition = this.missionData.getTimeCondition();
        var source = new LimitedTimeTrackerModel(this.tracker, this.time);
        if(condition.test(source)) {
            return 0L;
        }
        var result = condition.resolve(source);
        if (result.isEmpty()) {
            return null;
        }
        long left = Long.MAX_VALUE;
        for(var r : result) {
            if (r.getLow() > 0 && r.getLow() < left) {
                left = r.getLow();
            }
        }
        return left == Long.MAX_VALUE ? null : left;
    }

    public static final class PlayerRelatedCache {

        public final Player player;
        private final @Nullable IEssentials ess;
        private @Nullable User essUser;

        public PlayerRelatedCache(Player player, @Nullable Plugin essentialsPlugin) {
            this.player = player;
            if (essentialsPlugin instanceof IEssentials ess) {
                this.ess = ess;
            } else {
                this.ess = null;
            }
        }

        // can only be called from game-loop thread
        public @Nullable User getEssUser() {
            if (this.essUser == null && this.ess != null) {
                this.essUser = this.ess.getUser(this.player);
            }
            return this.essUser;
        }
    }

    public interface IWaitTimeCollection {

        void waitFor(long timeout);
    }
}
