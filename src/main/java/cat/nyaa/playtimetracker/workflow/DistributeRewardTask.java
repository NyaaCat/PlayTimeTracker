package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.config.data.CommandRewardData;
import cat.nyaa.playtimetracker.config.data.EcoRewardData;
import cat.nyaa.playtimetracker.executor.IOnceTrigger;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.reward.CommandReward;
import cat.nyaa.playtimetracker.reward.EcoReward;
import cat.nyaa.playtimetracker.reward.IReward;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/// sync task (first step)
public class DistributeRewardTask implements ITask {

    private final static Logger logger = LoggerUtils.getPluginLogger();
    
    private final Context context;
    private final String mission;
    private final IReward reward;
    private final UpdateWorkflow workflow;
    private final @Nullable IOnceTrigger groupOp;
    private int step;

    public DistributeRewardTask(Context context, UpdateWorkflow workflow, String mission, Object rewardData, @Nullable IOnceTrigger groupOp) {
        this.context = context;
        this.workflow = workflow;
        this.mission = mission;
        this.reward = createReward(mission, rewardData);
        this.groupOp = groupOp;
        this.step = 0;
        this.workflow.retain();
    }

    boolean isRewardValid() {
        return this.reward != null;
    }

    @Override
    public void execute(@Nullable Long tick) {
        if (this.step == 0 && tick == null) {
            // ensure step 0 is executed in sync thread
            this.context.getExecutor().sync(this);
            return;
        }
        try {
            logger.trace("DistributeRewardTask execute START; step:{} player={},mission={} reward={}", this.step, this.workflow.getPlayerContext().getUUID(), this.mission, this.reward);
            switch (this.step) {
                case 0 -> this.syncHandleStep1(tick);
                case 1 -> this.asyncHandleStep2();
                default -> throw new IllegalStateException();
            }
            logger.trace("DistributeRewardTask execute END; next:{}", this.step);
        } finally {
            if (this.step == 0xFF){
                // step 0xFF means the task is finished
                this.workflow.release(tick);
            }
        }
    }

    private void syncHandleStep1(long tick) {
        // step 1: prepare reward in game-loop
        boolean prepared = this.reward.prepare(this.mission, this.workflow.getTime().getTimestamp(), this.workflow.getPlayerContext().getPlayer(tick), this.context.getPlugin());
        if (!prepared) {
            logger.warn("DistributeRewardTask execute Failed to prepare reward for player={}, mission={}, reward={} time={}", this.workflow.getPlayerContext().getUUID(), this.mission, this.reward, this.workflow.getTime().getTimestamp());
            this.step = 0xFF; // cancel the task
            return;
        }

        this.step = 1; // proceed to next step
        this.context.getExecutor().async(this);
    }

    private void asyncHandleStep2() {
        // step 2: push reward asynchronously
        var rewardsConnection = context.getRewardsConnection();
        rewardsConnection.addReward(this.workflow.getPlayerContext().getUUID(), this.mission, this.reward, this.workflow.getTime().getTimestamp());
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
