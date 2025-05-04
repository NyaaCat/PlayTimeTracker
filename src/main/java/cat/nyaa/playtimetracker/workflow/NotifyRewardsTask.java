package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.utils.PlaceholderAPIUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class NotifyRewardsTask implements ITask {

    private final Player player;
    private final String mission;

    public NotifyRewardsTask(Player player, String mission) {
        this.player = player;
        this.mission = mission;
    }

    @Override
    public int execute(Workflow workflow, boolean executeInGameLoop) throws Exception {
        if (!executeInGameLoop) {
            return -1;
        }
        if (!this.player.isOnline()) {
            return 0;
        }
        this.doNotify();
        return 0;
    }

    private void doNotify() {
        String command = PlaceholderAPIUtils.setPlaceholders(this.player, I18n.format("message.mission.notify.command", this.mission));
        String msg = PlaceholderAPIUtils.setPlaceholders(this.player, I18n.format("message.mission.notify.msg", this.mission));
        var builder = Component.text();
        builder.append(LegacyComponentSerializer.legacySection().deserialize(msg));
        builder.append(
                Component.text()
                        .content(command)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)
                        )
        );
        this.player.sendMessage(builder.build());
    }


    public static class Once implements DistributeRewardTask.INotifyReward {
        private NotifyRewardsTask instance;

        public Once() {

        }


        @Override
        public void notify(Player player, String mission, Workflow workflow) {
            // TODO: consider thread safety ?
            if (this.instance == null) {
                this.instance = new NotifyRewardsTask(player, mission);
                workflow.addNextWorkStep(this.instance, true);
            }
        }
    }
}
