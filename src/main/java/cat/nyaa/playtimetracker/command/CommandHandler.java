package cat.nyaa.playtimetracker.command;

import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.PlayerAFKManager;
import cat.nyaa.playtimetracker.Utils.CommandUtils;
import cat.nyaa.playtimetracker.Utils.TimeUtils;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandHandler extends CommandReceiver {
    private final I18n i18n;
    private final PlayTimeTracker plugin;

    /**
     * @param plugin for logging purpose only
     * @param _i18n  i18n
     */
    public CommandHandler(PlayTimeTracker plugin, I18n _i18n) {
        super(plugin, _i18n);
        this.i18n = _i18n;
        this.plugin = plugin;
    }

    @SubCommand(value = "view", permission = "ptt.command.view", isDefaultCommand = true)
    public void view(CommandSender sender, Arguments args) {
        if (PlayTimeTracker.getInstance() == null || PlayTimeTracker.getInstance().getTimeRecordManager() == null) {
            I18n.send(sender, "command.view.err");
            return;
        }
        String targetName = args.next();
        UUID targetUUID = null;
        if (targetName != null) {
            String otherPermission = "ptt.command.view.other";
            if (!sender.hasPermission(otherPermission)) {
                I18n.send(sender, "command.view.no_view_other_permission", otherPermission);
                return;
            }
            targetUUID = CommandUtils.getPlayerUUIDByStr(targetName, sender);
        } else {
            if (sender instanceof Player) {
                targetUUID = ((Player) sender).getUniqueId();
                targetName = sender.getName();
            }
        }
        if (targetUUID == null) {
            I18n.send(sender, "command.view.invalid_target", targetName);
            return;
        }

        TimeTrackerDbModel timeTrackerDbModel = PlayTimeTracker.getInstance().getTimeRecordManager().getPlayerTimeTrackerDbModel(targetUUID);
        if (timeTrackerDbModel == null) {
            I18n.send(sender, "command.view.no_record", targetName);
            return;
        }
        I18n.send(sender, "command.view.query_title", targetName, targetUUID.toString());
        I18n.send(sender, "command.view.last_seen", TimeUtils.dateFormat(timeTrackerDbModel.getLastSeen()));
        I18n.send(sender, "command.view.daily_time", TimeUtils.timeFormat(timeTrackerDbModel.getDailyTime()));
        I18n.send(sender, "command.view.weekly_time", TimeUtils.timeFormat(timeTrackerDbModel.getWeeklyTime()));
        I18n.send(sender, "command.view.monthly_time", TimeUtils.timeFormat(timeTrackerDbModel.getMonthlyTime()));
        I18n.send(sender, "command.view.total_time", TimeUtils.timeFormat(timeTrackerDbModel.getTotalTime()));
    }

    @SubCommand(value = "afkstat", permission = "ptt.command.afkstat")
    public void afkStatus(CommandSender sender, Arguments args) {
        Player player = args.nextPlayer();

        if (!PlayerAFKManager.isAFK(player.getUniqueId())) {
            I18n.send(sender, "command.afkstst.no_afk", player.getName());
            return;
        }
        if (PlayTimeTracker.getInstance() == null || PlayTimeTracker.getInstance().getAfkManager() == null) {
            I18n.send(sender, "command.afkstst.not_found", player.getName());
            return;
        }
        if (PlayerAFKManager.isEssAfk(player.getUniqueId())) {
            I18n.send(sender, "command.afkstst.ess_afk", player.getName());
            return;
        }
        long afkTime = PlayTimeTracker.getInstance().getAfkManager().getAfkTime(player.getUniqueId());
        long lastActivity = PlayTimeTracker.getInstance().getAfkManager().getlastActivity(player.getUniqueId());
        I18n.send(sender, "command.afkstst.info", player.getName(), TimeUtils.dateFormat(lastActivity), TimeUtils.timeFormat(afkTime));
    }

    @SubCommand(value = "reload", permission = "ptt.command.reload")
    public void reload(CommandSender sender, Arguments args) {
        I18n.send(sender, "command.reload.start");
        if (PlayTimeTracker.getInstance() != null) {
            try {
                PlayTimeTracker.getInstance().onReload();
            } catch (Exception e) {
                e.printStackTrace();
                PlayTimeTracker.getInstance().getPluginLoader().disablePlugin(PlayTimeTracker.getInstance());
                I18n.send(sender, "command.reload.err");
            }
        } else {
            I18n.send(sender, "command.reload.err");
        }
        I18n.send(sender, "command.reload.finish");
    }

    public I18n getI18n() {
        return i18n;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    public PlayTimeTracker getPlugin() {
        return plugin;
    }


}
