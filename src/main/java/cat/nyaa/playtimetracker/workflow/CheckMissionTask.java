package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.condition.Range;
import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.executor.IFinalTrigger;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import org.slf4j.Logger;

import java.time.Duration;

public class CheckMissionTask implements ITask {

    private final static Logger logger = LoggerUtils.getPluginLogger();

    private final Context context;
    private final PlayerContext playerContext;
    private final String missionName;
    private final MissionData missionData;
    private final LimitedTimeTrackerModel tracker;
    private final IScheduler scheduler;
    private int step;

    public CheckMissionTask(Context context, PlayerContext playerContext, LimitedTimeTrackerModel tracker, String missionName, MissionData missionData, IScheduler scheduler) {
        this.context = context;
        this.playerContext = playerContext;
        this.missionName = missionName;
        this.missionData = missionData;
        this.tracker = tracker;
        this.scheduler = scheduler;
        this.step = 0;
        this.scheduler.retain();
    }

    @Override
    public void execute(Long tick) {
        try {
            logger.trace("CheckMissionTask execute@{} START; step:{} player={},mission={}", tick, this.step, this.playerContext.getUUID(), this.missionName);
            switch (this.step) {
                case 0 -> this.step = this.syncHandleStep1(tick);
                case 1 -> this.step = this.asyncHandleStep2();
                default -> throw new IllegalStateException();
            }
            logger.trace("CheckMissionTask execute END; next:{}", this.step);
        } finally {
            if (this.step == 0xFF){
                // step 0xFF means the task is finished
                this.scheduler.release();
            }
        }
    }

    private int syncHandleStep1(long tick) {
        // step 1: filter in game-loop

        if (!this.checkInGroup(tick)) {
            return 0xFF;
        }

        this.context.getExecutor().async(this);
        return 1;
    }

    private int asyncHandleStep2() {
        // step 2: check asynchronously

        if (!this.checkUncompleted()) {
            return 0xFF;
        }

        var waitTime = this.checkMissionTimeCondition();
        logger.trace("CheckMissionTask wait {}ms to complete", waitTime);
        if (waitTime == null) {
            // impossible to complete
            return 0xFF;
        }

        if (waitTime.isPositive()) {
            // wait for the time condition
            this.scheduler.record(this.missionName, waitTime);
            return 0xFF;
        }

        // push reward and notify
        var notifyRewardsTask = this.missionData.notify ? new NotifyRewardTask(this.context, this.playerContext, this.missionName) : null;
        for (var e : this.missionData.getSortedRewardList()) {
            var rewardTask = new DistributeRewardTask(this.context, this.playerContext, this.missionName, e, this.tracker.time, notifyRewardsTask);
            if (rewardTask.isRewardValid()) {
                this.context.getExecutor().sync(rewardTask);
            }
        }
        return 0xFF;
    }

    private boolean checkInGroup(long tick) {
        if (this.missionData.group != null && !this.missionData.group.isEmpty()) {
            var essUser = this.playerContext.getEssUser(tick);
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
        var model = this.context.getCompletedMissionConnection().getPlayerCompletedMission(this.playerContext.getUUID(), this.missionName);
        return model == null;
    }

    // @return the time to wait; None if impossible to complete; 0 if already completed
    private Duration checkMissionTimeCondition() {
        var condition = this.context.buildMissionTimeCondition(this.missionData);
        var source = this.tracker;
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

    public interface IScheduler extends IFinalTrigger {

        void record(String mission, Duration waitTime);
    }
}
