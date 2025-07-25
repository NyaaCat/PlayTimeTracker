package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ListRewardTask implements ITask {

    private final Logger logger = LoggerUtils.getPluginLogger();

    private final Context context;
    private final PlayerContext playerContext;
    private final @Nullable String mission;
    private int step;
    private Object2IntMap<String> rewardsCount;
    private int rewardCount;
    private boolean error;

    public ListRewardTask(Context context, PlayerContext playerContext, @Nullable String mission) {
        this.context = context;
        this.playerContext = playerContext;
        this.mission = mission;
        this.step = 0;
        this.rewardsCount = null;
        this.rewardCount = 0;
        this.error = false;
    }

    @Override
    public void execute(Long tick) {
        logger.trace("ListRewardTask execute@{} START; step:{} player={}, mission={}", tick, this.step, this.playerContext.getUUID(), this.mission);
        switch (this.step) {
            case 0 -> this.step = this.asyncHandleStep1();
            case 1 -> this.step = this.syncHandleStep2(tick);
            default -> throw new IllegalStateException();
        }
        logger.trace("ListRewardTask execute END; next:{}", this.step);
    }

    private int asyncHandleStep1() {
        // step 1: query database
        try {
            if (this.mission == null) {
                this.rewardsCount = this.context.getRewardsConnection().countRewards(this.playerContext.getUUID());
                this.rewardCount = this.rewardsCount.values().intStream().sum();
            } else {
                this.rewardCount = this.context.getRewardsConnection().countReward(this.playerContext.getUUID(), this.mission);
            }
        } catch (Exception e) {
            logger.error("ListRewardTask query failed for player={}, mission={}", this.playerContext.getUUID(), this.mission, e);
            this.error = true;
        } finally {
            this.context.getExecutor().async(this);
        }
        return 1;
    }

    private int syncHandleStep2(long tick) {
        // step 2: build response and send to player
        var player = this.playerContext.getPlayer(tick);
        if (player == null || !player.isOnline()) {
            logger.warn("ListRewardTask execute@{} player={} is offline", tick, this.playerContext.getUUID());
            return 0xFF; // mark as failed
        }
        if (this.error) {
            I18n.send(player, "command.listrewards.err");
            return 0xFF; // mark as failed
        }
        if (this.rewardsCount != null) {
            if(this.rewardsCount.isEmpty()) {
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
            if (this.rewardCount == 0) {
                I18n.send(player, "command.listrewards.empty", this.mission);
            } else {
                I18n.send(player, "command.listrewards.show", this.rewardCount, this.mission);
            }
        }
        return 0xFF;
    }
}
