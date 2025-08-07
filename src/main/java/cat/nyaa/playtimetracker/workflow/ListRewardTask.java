package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/// async task (first step)
public class ListRewardTask implements ITask {

    private final Logger logger = LoggerUtils.getPluginLogger();

    private final Context context;
    private final PlayerContext playerContext;
    private final @Nullable String mission;
    private final boolean notifyEmpty;
    private int step;
    private Object2IntMap<String> rewardsCount;
    private int rewardCount;

    public ListRewardTask(Context context, PlayerContext playerContext, @Nullable String mission, boolean notifyEmpty) {
        this.context = context;
        this.playerContext = playerContext;
        this.mission = mission;
        this.notifyEmpty = notifyEmpty;
        this.step = 0;
        this.rewardsCount = null;
        this.rewardCount = -1;
    }

    @Override
    public void execute(@Nullable Long tick) {
        logger.trace("ListRewardTask execute START; step:{} player={}, mission={}", this.step, this.playerContext.getUUID(), this.mission);
        switch (this.step) {
            case 0 -> this.asyncHandleStep1();
            case 1 -> this.syncHandleStep2(tick);
            default -> throw new IllegalStateException();
        }
        logger.trace("ListRewardTask execute END; next:{}", this.step);
    }

    private void asyncHandleStep1() {
        // step 1: query database
        if (this.mission == null) {
            // list all rewards
            this.rewardsCount = this.context.getRewardsConnection().countRewards(this.playerContext.getUUID()); // return null if query failed
            if (this.rewardsCount != null) {
                this.rewardCount = this.rewardsCount.values().intStream().sum();
            }
        } else {
            // list rewards for a specific mission
            this.rewardCount = this.context.getRewardsConnection().countReward(this.playerContext.getUUID(), this.mission); // return 0 if query failed
        }

        this.step = 1;
        this.context.getExecutor().sync(this);
    }

    private void syncHandleStep2(long tick) {
        // step 2: build response and send to player
        var player = this.playerContext.getPlayer(tick);
        if (player == null || !player.isOnline()) {
            logger.warn("ListRewardTask execute@{} player={} is offline", tick, this.playerContext.getUUID());
            this.step = 0xFF; // mark as failed
            return;
        }
        if (
            (this.mission == null && this.rewardsCount == null) ||
            (this.mission != null && this.rewardCount < 0)
        ) {
            I18n.send(player, "command.listrewards.err");
            this.step = 0xFF; // mark as failed
            return;
        }
        if (this.mission == null) {
            // list all rewards
            if(this.rewardsCount.isEmpty() && this.notifyEmpty) {
                I18n.send(player, "command.listrewards.empty_all");
            } else {
                String header = I18n.format("command.listrewards.show_all", this.rewardCount);
                var builder = Component.text();
                builder.append(LegacyComponentSerializer.legacySection().deserialize(header));
                for (var entry : this.rewardsCount.object2IntEntrySet()) {
                    String line = I18n.format("command.listrewards.show_item", entry.getKey(), entry.getIntValue());
                    builder.append(Component.newline());
                    builder.append(LegacyComponentSerializer.legacySection().deserialize(line));
                }
                player.sendMessage(builder.build());
            }
        } else {
            // list rewards for a specific mission
            if (this.rewardCount == 0 && this.notifyEmpty) {
                I18n.send(player, "command.listrewards.empty", this.mission);
            } else {
                I18n.send(player, "command.listrewards.show", this.rewardCount, this.mission);
            }
        }
        this.step = 0xFF;
    }
}
