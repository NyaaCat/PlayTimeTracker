package cat.nyaa.playtimetracker.command;

import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.playtimetracker.*;
import cat.nyaa.playtimetracker.command.sub.ResetCommand;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.utils.CommandUtils;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import cat.nyaa.playtimetracker.workflow.DisplayNextMissionMode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.UUID;

public class CommandHandler extends CommandReceiver {

    private static final Logger logger = LoggerUtils.getPluginLogger();

    private final PlayTimeTracker plugin;

    @SubCommand(value = "reset", permission = "ptt.command.reset")
    public ResetCommand resetCommand;

    public CommandHandler(Plugin plugin, I18n _i18n) {
        super(plugin, _i18n);
        this.plugin = (PlayTimeTracker) plugin;
    }

    @SubCommand(value = "view", permission = "ptt.command.view", isDefaultCommand = true)
    public void view(CommandSender sender, Arguments args) {
        var controller = this.plugin.getController();
        if (controller == null) {
            I18n.send(sender, "command.view.err");
            return;
        }
        String targetName = args.next();
        DisplayNextMissionMode mode = DisplayNextMissionMode.First;
        if ("full".equals(targetName)) {
            targetName = args.next();
            mode = DisplayNextMissionMode.All;
        }
        if (targetName != null) {
            String otherPermission = "ptt.command.view.other";
            if (!sender.hasPermission(otherPermission)) {
                I18n.send(sender, "command.view.no_view_other_permission", otherPermission);
                return;
            }
            var obj = CommandUtils.getPlayerByStr(targetName, sender);
            switch (obj) {
                case Player player -> {
                    if (mode != DisplayNextMissionMode.All) {
                        var nextArgs = args.next();
                        if ("full".equals(nextArgs)) {
                            mode = DisplayNextMissionMode.All;
                        }
                    }
                    if (!controller.isRunning()) {
                        I18n.send(sender, "command.view.err");
                        return;
                    }
                    controller.viewOnlinePlayTime(player, sender, mode);
                }
                case UUID uuid -> {
                    if (!controller.isRunning()) {
                        I18n.send(sender, "command.view.err");
                        return;
                    }
                    controller.viewOfflinePlayTime(uuid, targetName, sender);
                }
                case null, default -> I18n.send(sender, "command.view.invalid_target", targetName);
            }
        } else if (sender instanceof Player player) {
            if (!controller.isRunning()) {
                I18n.send(sender, "command.view.err");
                return;
            }
            controller.viewOnlinePlayTime(player, sender, mode);
        } else {
            I18n.send(sender, "command.view.invalid_target", targetName);
        }
    }

    @SubCommand(value = "migration", permission = "ptt.command.migration")
    public void migration(CommandSender sender, Arguments args) {

        if (!args.nextString("").equals("confirm")) {
            I18n.send(sender, "command.migration.confirm");
            return;
        }
        var controller = this.plugin.getController();
        if (controller == null) {
            I18n.send(sender, "command.migration.err");
            return;
        }
        var conn = controller.getTimeTrackerConnection();
        if (conn == null) {
            I18n.send(sender, "command.migration.err");
            return;
        }
        File dbFile = new File(this.plugin.getDataFolder(), "database.yml");


        if (!dbFile.canRead()) {
            I18n.send(sender, "command.migration.can_not_read");
            return;
        }
        if (!dbFile.exists()) {
            I18n.send(sender, "command.migration.does_not_exist");
            return;
        }
        if (!dbFile.isFile()) {
            I18n.send(sender, "command.migration.nor_a_file");
            return;
        }

        ConfigurationSection cfg = YamlConfiguration.loadConfiguration(dbFile);
        for (String uuid_str : cfg.getKeys(false)) {
            try {
                ConfigurationSection sec = cfg.getConfigurationSection(uuid_str);
                //ZonedDateTime rec_lastSeen = ZonedDateTime.parse(sec.getString("last_seen"));
                UUID rec_id = UUID.fromString(uuid_str);
                ZonedDateTime rec_lastSeen = ZonedDateTime.parse(sec.getString("last_seen"));
                long rec_dailyTime = sec.getLong("daily_play_time");
                long rec_weeklyTime = sec.getLong("weekly_play_time");
                long rec_monthlyTime = sec.getLong("monthly_play_time");
                long rec_totalTime = sec.getLong("total_play_time");
                TimeTrackerDbModel trackerDbModel = new TimeTrackerDbModel();
                long timestamp = rec_lastSeen.toInstant().toEpochMilli();
                trackerDbModel.setLastUpdate(timestamp);
                trackerDbModel.setDailyTime(rec_dailyTime);
                trackerDbModel.setWeeklyTime(rec_weeklyTime);
                trackerDbModel.setMonthlyTime(rec_monthlyTime);
                trackerDbModel.setTotalTime(rec_totalTime);
                trackerDbModel.setLastSeen(timestamp);
                trackerDbModel.setPlayerUniqueId(rec_id);
                conn.insertPlayer(trackerDbModel);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(rec_id);
                var playerName = offlinePlayer.getName();
                I18n.send(sender, "command.migration.insert", playerName == null ? "{" + rec_id + "}" : playerName);
            } catch (Exception e) {
                logger.error("Error while migrating player data", e);
                sender.sendMessage(e.getLocalizedMessage());
            }
            I18n.send(sender, "command.migration.finish");
        }


    }

    @SubCommand(value = "afkstatus", permission = "ptt.command.afkstatus")
    public void afkStatus(CommandSender sender, Arguments args) {
        Player player = args.nextPlayer();

        if (!PlayerAFKManager.isAFK(player.getUniqueId())) {
            I18n.send(sender, "command.afkstatus.no_afk", player.getName());
            return;
        }
        if (PlayTimeTracker.getInstance() == null || PlayTimeTracker.getInstance().getAfkManager() == null) {
            I18n.send(sender, "command.afkstatus.not_found", player.getName());
            return;
        }
        if (PlayerAFKManager.isEssAfk(player.getUniqueId())) {
            I18n.send(sender, "command.afkstatus.ess_afk", player.getName());
            return;
        }
        long afkTime = PlayTimeTracker.getInstance().getAfkManager().getAfkTime(player.getUniqueId());
        long lastActivity = PlayTimeTracker.getInstance().getAfkManager().getLastActivity(player.getUniqueId());
        I18n.send(sender, "command.afkstatus.info", player.getName(), TimeUtils.dateFormat(lastActivity), TimeUtils.timeFormat(afkTime));
    }

    @SubCommand(value = "listrewards", alias = {"lsr"}, permission = "ptt.command.listrewards")
    public void listRewards(CommandSender sender, Arguments args) {
        var controller = this.plugin.getController();
        if (controller == null) {
            I18n.send(sender, "command.listrewards.err");
            return;
        }
        String missionName = args.nextString("all");
        if (!(sender instanceof Player player)) {
            I18n.send(sender, "command.only-player-can-do");
            return;
        }
        if ("all".equals(missionName)) {
            missionName = null; // null means list all rewards
        }
        if (!controller.isRunning()) {
            I18n.send(sender, "command.listrewards.err");
        }
        controller.listReward(player, missionName);
    }

    @SubCommand(value = "acquire", alias = {"ac"}, permission = "ptt.command.acquire")
    public void acquire(CommandSender sender, Arguments args) {
        var controller = this.plugin.getController();
        if (controller == null) {
            I18n.send(sender, "command.acquire.err");
            return;
        }
        String missionName = args.nextString("all");
        if (!(sender instanceof Player player)) {
            I18n.send(sender, "command.only-player-can-do");
            return;
        }
        if ("all".equals(missionName)) {
            missionName = null; // null means list all rewards
        }
        if (!controller.isRunning()) {
            I18n.send(sender, "command.acquire.err");
        }
        controller.acquireReward(player, missionName);
    }

    @SubCommand(value = "reload", permission = "ptt.command.reload")
    public void reload(CommandSender sender, Arguments args) {
        I18n.send(sender, "command.reload.start");
        if (this.plugin != null) {
            try {
                this.plugin.onReload();
            } catch (Exception e) {
                e.printStackTrace();
                //PlayTimeTracker.getInstance().getPluginLoader().disablePlugin(PlayTimeTracker.getInstance());
                I18n.send(sender, "command.reload.err");
            }
        } else {
            I18n.send(sender, "command.reload.err");
        }
        I18n.send(sender, "command.reload.finish");
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }
}
