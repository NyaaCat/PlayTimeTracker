package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.executor.AbstractOnceTrigger;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.PlaceholderAPIUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

public class NotifyRewardTask extends AbstractOnceTrigger implements ITask {

    private final static Logger logger = LoggerUtils.getPluginLogger();

    private final Context context;
    private final PlayerContext playerContext;
    private final String mission;

    public NotifyRewardTask(Context context, PlayerContext playerContext, String mission) {
        this.context = context;
        this.playerContext = playerContext;
        this.mission = mission;
    }

    @Override
    public void execute(Long tick) {
        logger.trace("NotifyRewardsTask execute@{} START player={}, mission={}", tick, this.playerContext.getUUID(), this.mission);
        final Player player = this.playerContext.getPlayer(tick);
        if (player == null || !player.isOnline()) {
            logger.warn("NotifyRewardsTask execute@{} player={} is offline, mission={}", tick, this.playerContext.getUUID(), this.mission);
        } else {
            this.doNotify(player);
        }
        logger.trace("NotifyRewardsTask executed End");
    }

    private void doNotify(Player player) {
        String command = PlaceholderAPIUtils.setPlaceholders(player, I18n.format("message.mission.notify.command", this.mission));
        String msg = PlaceholderAPIUtils.setPlaceholders(player, I18n.format("message.mission.notify.msg", this.mission));
        var builder = Component.text();
        builder.append(LegacyComponentSerializer.legacySection().deserialize(msg));
        builder.append(
                Component.text()
                        .content(command)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)
                        )
        );
        player.sendMessage(builder.build());
    }

    @Override
    protected void handle() {
        this.context.getExecutor().sync(this);
        logger.info("NotifyRewardsTask added to executor for player={}, mission={}", this.playerContext.getUUID(), this.mission);
    }
}
