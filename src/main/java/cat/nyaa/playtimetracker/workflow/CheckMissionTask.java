package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.condition.Range;
import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Duration;

/// async task (first step)
public class CheckMissionTask implements ITask {

    private final static Logger logger = LoggerUtils.getPluginLogger();

    private final Context context;
    private final UpdateWorkflow workflow;
    private final String missionName;
    private final MissionData missionData;
    private final LimitedTimeTrackerModel model;
    private int step;

    public CheckMissionTask(Context context, UpdateWorkflow workflow, String missionName, MissionData missionData) {
        this.context = context;
        this.workflow = workflow;
        this.missionName = missionName;
        this.missionData = missionData;
        var model0 = this.workflow.getModel();
        assert model0 != null;
        this.model = new LimitedTimeTrackerModel(model0, this.workflow.getTime());
        this.step = 0;
        this.workflow.retain();
    }

    @Override
    public void execute(@Nullable Long tick) {
        if (this.step == 0 && tick == null) {
            // ensure step 0 is executed in sync thread
            this.context.getExecutor().sync(this);
            return;
        }
        try {
            logger.trace("CheckMissionTask execute START; step:{} player={},mission={}", this.step, this.workflow.getPlayerContext().getUUID(), this.missionName);
            switch (this.step) {
                case 0 -> this.syncHandleStep1(tick);
                case 1 -> this.asyncHandleStep2();
                default -> throw new IllegalStateException();
            }
            logger.trace("CheckMissionTask execute END; next:{}", this.step);
        } finally {
            if (this.step == 0xFF){
                // step 0xFF means the task is finished
                this.workflow.release(tick);
            }
        }
    }

    private void syncHandleStep1(long tick) {
        // step 1: filter in game-loop

        if (!this.checkInGroup(tick)) {
            this.step = 0xFF;
            return;
        }

        this.step = 1; // proceed to next step
        this.context.getExecutor().async(this);
    }

    private void asyncHandleStep2() {
        // step 2: check asynchronously

        if (!this.checkUncompleted()) {
            this.step = 0xFF; // already completed
            return;
        }

        var waitTime = this.checkMissionTimeCondition();
        logger.trace("CheckMissionTask wait {}ms to complete", waitTime);
        if (waitTime == null) {
            // impossible to complete
            this.step = 0xFF;
            return;
        }

        if (waitTime.isPositive()) {
            // wait for the time condition
            this.workflow.record(this.missionName, waitTime);
            this.step = 0xFF;
            return;
        }

        this.markCompleted();

        // push reward and notify
        var notifyRewardsTask = this.missionData.notify ? new NotifyRewardTask(this.context, this.workflow.getPlayerContext(), this.missionName) : null;
        for (var e : this.missionData.getSortedRewardList()) {
            var rewardTask = new DistributeRewardTask(this.context, this.workflow, this.missionName, e, notifyRewardsTask);
            if (rewardTask.isRewardValid()) {
                this.context.getExecutor().sync(rewardTask);
            }
        }

        this.step = 0xFF; // task finished
    }

    private boolean checkInGroup(long tick) {
        if (this.missionData.group != null && !this.missionData.group.isEmpty()) {
            var essUser = this.workflow.getPlayerContext().getEssUser(tick);
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

    private boolean checkUncompleted() {
        var model = this.context.getCompletedMissionConnection().getPlayerCompletedMission(this.workflow.getPlayerContext().getUUID(), this.missionName);
        if (model == null) {
            return true;
        }
        var time = this.model.time;
        if (model.lastCompletedTime < time.getSameDayStart() && this.missionData.resetDaily) {
            return true;
        }
        if (model.lastCompletedTime < time.getSameWeekStart() && !this.missionData.resetWeekly) {
            return true;
        }
        if (model.lastCompletedTime < time.getSameMonthStart() && !this.missionData.resetMonthly) {
            return true;
        }
        return false;
    }

    private void markCompleted() {
        this.context.getCompletedMissionConnection().writeMissionCompleted(this.workflow.getPlayerContext().getUUID(), this.missionName, this.model.time.getTimestamp());
    }

    // @return the time to wait; None if impossible to complete; 0 if already completed
    private Duration checkMissionTimeCondition() {
        var condition = this.context.buildMissionTimeCondition(this.missionData);
        var source = this.model;
        if(condition.test(source)) {
            logger.trace("CheckMissionTask checkMissionTimeCondition condition.test: passed");
            return Duration.ZERO;
        }
        var result = condition.resolve(source);
        if (logger.isTraceEnabled()) {
            logger.trace("CheckMissionTask checkMissionTimeCondition condition.resolve: result={}", fmtRanges(result));
        }
        if (result.isEmpty()) {
            return null;
        }

        return source.analyzeResolved(result);
    }

    private static String fmtRanges(Iterable<Range> ranges) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var r : ranges) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append('[').append(r.getLow()).append(',').append(r.getHigh()).append(']');
        }
        return sb.toString();
    }
}
