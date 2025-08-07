package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.config.data.CommandRewardData;
import cat.nyaa.playtimetracker.config.data.EcoRewardData;
import cat.nyaa.playtimetracker.executor.IOnceTrigger;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.reward.CommandReward;
import cat.nyaa.playtimetracker.reward.EcoReward;
import cat.nyaa.playtimetracker.reward.IReward;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/// sync task (first step)
public class DistributeRewardTask implements ITask {

    private final static Logger logger = LoggerUtils.getPluginLogger();
    
    private final Context context;
    private final PlayerContext playerContext;
    private final String mission;
    private final IReward reward;
    private final PiecewiseTimeInfo time;
    private final @Nullable IOnceTrigger groupOp;
    private int step;

    public DistributeRewardTask(Context context, PlayerContext playerContext, String mission, Object rewardData, PiecewiseTimeInfo time, @Nullable IOnceTrigger groupOp) {
        this.context = context;
        this.playerContext = playerContext;
        this.mission = mission;
        this.time = time;
        this.reward = createReward(mission, rewardData);
        this.groupOp = groupOp;
        this.step = 0;
    }

    boolean isRewardValid() {
        return this.reward != null;
    }

    @Override
    public void execute(@Nullable Long tick) {
        logger.trace("DistributeRewardTask execute START; step:{} player={},mission={} reward={}", this.step, this.playerContext.getUUID(), this.mission, this.reward);
        switch (this.step) {
            case 0 -> this.syncHandleStep1(tick);
            case 1 -> this.asyncHandleStep2();
            default -> throw new IllegalStateException();
        }
        logger.trace("DistributeRewardTask execute END; next:{}", this.step);
    }

    private void syncHandleStep1(long tick) {
        // step 1: prepare reward in game-loop
        boolean prepared = this.reward.prepare(this.mission, this.time.getTimestamp(), this.playerContext.getPlayer(tick), this.context.getPlugin());
        if (!prepared) {
            logger.warn("DistributeRewardTask execute Failed to prepare reward for player={}, mission={}, reward={} time={}", this.playerContext.getUUID(), this.mission, this.reward, this.time.getTimestamp());
            this.step = 0xFF; // cancel the task
            return;
        }

        this.step = 1; // proceed to next step
        this.context.getExecutor().async(this);
    }

    private void asyncHandleStep2() {
        // step 2: push reward asynchronously
        var rewardsConnection = context.getRewardsConnection();
        rewardsConnection.addReward(this.playerContext.getUUID(), this.mission, this.reward, this.time.getTimestamp());
        if (this.groupOp != null) {
            this.groupOp.trigger(null);
        }
        this.step = 0xFF; // task finished
    }

    private @Nullable IReward createReward(String mission, Object rewardData) {
        if (rewardData instanceof EcoRewardData ecoRewardData) {
            return new EcoReward(ecoRewardData);
        }
        if (rewardData instanceof CommandRewardData commandRewardData) {
            return new CommandReward(commandRewardData);
        }
        return null;
    }
}
