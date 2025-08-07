package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.db.model.RewardDbModel;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

/// async task (first step)
public class AcquireRewardTask implements ITask {

    public final Logger logger = LoggerUtils.getPluginLogger();

    private final Context context;
    private final PlayerContext playerContext;
    private final @Nullable String mission;
    private List<RewardDbModel> rewards; // rewards to be acquired, query from database in first step
    private IntCollection rewardIdList; // reward IDs have been acquired and need to be removed from database
    private int step;

    public AcquireRewardTask(Context context, PlayerContext playerContext, @Nullable String mission) {
        this.context = context;
        this.playerContext = playerContext;
        this.mission = mission;
        this.rewards = null;
        this.step = 0;
    }

    @Override
    public void execute(@Nullable Long tick) {
        logger.trace("AcquireRewardTask execute START; step:{} player={},mission={}", this.step, this.playerContext.getUUID(), this.mission);
        switch (this.step) {
            case 0 -> this.asyncHandleStep1();
            case 1 -> this.syncHandleStep2(tick);
            case 2 -> this.asyncHandleStep3();
            default -> throw new IllegalStateException();
        }
        logger.trace("AcquireRewardTask execute END; next:{}", this.step);
    }

    private void asyncHandleStep1() {
        this.rewards = this.context.getRewardsConnection().getRewards(this.playerContext.getUUID(), this.mission); // return null if query failed
        this.step =  1; // proceed to next step
        this.context.getExecutor().sync(this);
    }

    private void syncHandleStep2(long tick) {
        var player = this.playerContext.getPlayer(tick);
        if (player == null || !player.isOnline()) {
            logger.warn("AcquireRewardTask player {} is offline, cannot acquire rewards", this.playerContext.getUUID());
            this.step = 0xFF; // task finished
            return;
        }
        if (this.rewards == null) {
            I18n.send(player, "command.acquire.err");
            this.step = 0xFF; // task finished
            return;
        }
        if (this.rewards.isEmpty()) {
            if(this.mission == null) {
                I18n.send(player, "command.acquire.empty");
            } else {
                I18n.send(player, "command.acquire.not_found", this.mission);
            }
            this.step = 0xFF; // task finished
        } else {
            IntArrayList rewardIdList = new IntArrayList(this.rewards.size());
            ObjectArrayList<Component> outputMessages = new ObjectArrayList<>(16);
            for (RewardDbModel reward : this.rewards) {
                Boolean distributeRet = reward.getReward().distribute(player, this.context.getPlugin(), outputMessages);
                if(distributeRet == null) {
                    logger.warn("AcquireRewardTask player {} blocked acquire reward {}", this.playerContext.getUUID(), reward.getId());
                    // TODO: send message to player?
                    break;
                } else {
                    var builder = Component.text();
                    if (distributeRet) {
                        rewardIdList.add(reward.getId());
                        // TODO: I18n
                        String successText = I18n.format("command.acquire.success", reward.getRewardName());
                        builder.append(LegacyComponentSerializer.legacySection().deserialize(successText));
                    } else {
                        String failText = I18n.format("command.acquire.failed", reward.getRewardName());
                        builder.append(LegacyComponentSerializer.legacySection().deserialize(failText));
                    }
                    if(!outputMessages.isEmpty()) {
                        builder.append(Component.text(' '));
                        for (Component outputMessage : outputMessages) {
                            builder.append(outputMessage);
                        }
                    }
                    player.sendMessage(builder.build());
                }
                outputMessages.clear();
            }
            if (rewardIdList.isEmpty()) {
                this.step = 0xFF; // task finished, no rewards acquired
                return;
            }
            this.rewardIdList = rewardIdList;
            this.step = 2; // proceed to next step
            this.context.getExecutor().async(this);
        }
    }

    private void asyncHandleStep3() {
        if (this.rewardIdList == null || this.rewardIdList.isEmpty()) {
            this.step = 0xFF; // task finished
            return;
        }
        this.context.getRewardsConnection().removeRewards(this.rewardIdList);
        logger.info("AcquireRewardTask player {} has acquired {} rewards", this.playerContext.getUUID(), this.rewardIdList.size());
        this.step = 0xFF; // task finished
    }
}
