package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.config.data.CommandRewardData;
import cat.nyaa.playtimetracker.config.data.EcoRewardData;
import cat.nyaa.playtimetracker.reward.CommandReward;
import cat.nyaa.playtimetracker.reward.EcoReward;
import cat.nyaa.playtimetracker.reward.IReward;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class DistributeRewardTask implements ITask {

    private final Player player;
    private final String mission;
    private final IReward reward;
    private final PiecewiseTimeInfo time;
    private final INotifyReward rewardNotifier;
    private int step;

    public DistributeRewardTask(Player player, String mission, Object rewardData, PiecewiseTimeInfo time, INotifyReward rewardNotifier) {
        this.player = player;
        this.mission = mission;
        this.time = time;
        this.reward = createReward(mission, rewardData);
        this.rewardNotifier = rewardNotifier;
        this.step = 0;
    }

    boolean isRewardValid() {
        return this.reward != null;
    }

    @Override
    public int execute(Workflow workflow, boolean executeInGameLoop) throws Exception {
        switch (this.step) {
            case 0 -> {
                if (!executeInGameLoop) {
                    return -1;
                }
                // step 1: prepare reward in game-loop
                boolean prepared = reward.prepare(this.mission, this.time.getTimestamp(), this.player, workflow.getPlugin());
                if (!prepared) {
                    // TODO: log
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
                // step 2: push reward asynchronously
                try {
                    var rewardsConnection = workflow.getRewardsConnection();
                    rewardsConnection.addReward(this.player.getUniqueId(), this.mission, this.reward, this.time.getTimestamp());
                } catch (Exception e) {
                    // TODO: log
                    this.step = 0xFF;
                    return -3;
                }
                this.rewardNotifier.notify(this.player, this.mission, workflow);
                this.step = 0xFF;
                return 0;
            }
            default -> {
                return -2;
            }
        }
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


    public interface INotifyReward {
        void notify(final Player player, final String mission, Workflow workflow);
    }
}
