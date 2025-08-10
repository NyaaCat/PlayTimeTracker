package cat.nyaa.playtimetracker.command.sub;

import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.utils.CommandUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class ResetCommand extends CommandReceiver {

    private final PlayTimeTracker plugin;

    /**
     * @param plugin
     * @param _i18n  i18n
     */
    public ResetCommand(Plugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
        this.plugin = (PlayTimeTracker) plugin;
    }

    @SubCommand(value = "mission", permission = "ptt.command.reset.mission")
    public void resetMission(CommandSender sender, Arguments args) {
        var controller = this.plugin.getController();
        if (controller == null) {
            I18n.send(sender, "command.reset.mission.err");
            return;
        }
        String playerName = args.nextString();
        String missionName = args.nextString();
        if ("all".equals(missionName)) {
            missionName = null; // reset all missions if "all" is specified
        }
        var obj = CommandUtils.getPlayerByStr(playerName, sender);
        switch (obj) {
            case Player player -> {
                if (!controller.isRunning()) {
                    I18n.send(sender, "command.reset.mission.err", playerName);
                    return;
                }
                controller.resetMissionForOnlinePlayer(player, sender, missionName);
            }
            case UUID uuid -> {
                if (!controller.isRunning()) {
                    I18n.send(sender, "command.reset.mission.err", playerName);
                    return;
                }
                controller.resetMissionForOfflinePlayer(uuid, playerName, sender, missionName);
            }
            case null, default -> I18n.send(sender, "command.reset.mission.target_not_found", playerName);
        }
    }

    @SubCommand(value = "time", permission = "ptt.command.reset.time")
    public void resetTime(CommandSender sender, Arguments args) {
        var controller = this.plugin.getController();
        if (controller == null) {
            I18n.send(sender, "command.reset.time.err");
            return;
        }
        String playerName = args.nextString();
        var obj = CommandUtils.getPlayerByStr(playerName, sender);
        switch (obj) {
            case Player player -> {
                if (!controller.isRunning()) {
                    I18n.send(sender, "command.reset.time.err", playerName);
                    return;
                }
                controller.resetTimeForOnlinePlayer(player, sender);
            }
            case UUID uuid -> {
                if (!controller.isRunning()) {
                    I18n.send(sender, "command.reset.time.err", playerName);
                    return;
                }
                controller.resetTimeForOfflinePlayer(uuid, playerName, sender);
            }
            case null, default -> I18n.send(sender, "command.reset.time.target_not_found", playerName);
        }
    }

    @Override
    public String getHelpPrefix() {
        return "reset";
    }
}
