package cat.nyaa.playtimetracker.command;

import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.playtimetracker.*;
import cat.nyaa.playtimetracker.command.sub.ResetCommand;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.utils.CommandUtils;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

public class CommandHandler extends CommandReceiver {
    private final I18n i18n;
    private final PlayTimeTracker plugin;


    @SubCommand(value = "reset", permission = "ptt.command.reset")
    public ResetCommand resetCommand;

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

    @SubCommand(value = "migration", permission = "ptt.command.migration")
    public void migration(CommandSender sender, Arguments args) {
        if (!args.nextString("").equals("confirm")) {
            I18n.send(sender, "command.migration.confirm");
            return;
        }
        TimeRecordManager timeRecordManager = plugin.getTimeRecordManager();
        if (timeRecordManager == null) {
            I18n.send(sender, "command.migration.err");
            return;
        }
        File dbFile = new File(getPlugin().getDataFolder(), "database.yml");


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
                trackerDbModel.setLastSeen(rec_lastSeen.toInstant().toEpochMilli());
                trackerDbModel.setDailyTime(rec_dailyTime);
                trackerDbModel.setWeeklyTime(rec_weeklyTime);
                trackerDbModel.setMonthlyTime(rec_monthlyTime);
                trackerDbModel.setTotalTime(rec_totalTime);
                trackerDbModel.setPlayerUniqueId(rec_id);
                timeRecordManager.insertOrResetPlayer(trackerDbModel);
                timeRecordManager.insertOrResetPlayer(rec_id,TimeUtils.getUnixTimeStampNow());
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(rec_id);
                var playerName = offlinePlayer.getName();
                I18n.send(sender, "command.migration.insert", playerName == null ? "{" + rec_id + "}" : playerName);
            } catch (Exception e) {
                e.printStackTrace();
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
        long lastActivity = PlayTimeTracker.getInstance().getAfkManager().getlastActivity(player.getUniqueId());
        I18n.send(sender, "command.afkstatus.info", player.getName(), TimeUtils.dateFormat(lastActivity), TimeUtils.timeFormat(afkTime));
    }

    @SubCommand(value = "listrewards", alias = {"lsr"}, permission = "ptt.command.listrewards")
    public void listRewards(CommandSender sender, Arguments args) {
        String missionName = args.nextString("all");
        if (!(sender instanceof Player player)) {
            I18n.send(sender, "command.only-player-can-do");
            return;
        }
        PlayerMissionManager missionManager = plugin.getMissionManager();
        if (missionManager == null) {
            I18n.send(sender, "command.acquire.err");
            return;
        }
        missionManager.showPlayerRewards(player, missionName, false);
    }

    @SubCommand(value = "acquire", alias = {"ac"}, permission = "ptt.command.acquire")
    public void acquire(CommandSender sender, Arguments args) {
        String missionName = args.nextString("all");
        if (!(sender instanceof Player player)) {
            I18n.send(sender, "command.only-player-can-do");
            return;
        }
        PlayerMissionManager missionManager = plugin.getMissionManager();
        if (missionManager == null) {
            I18n.send(sender, "command.acquire.err");
            return;
        }
        missionManager.executeAcquire(player, missionName);
//        Set<String> missionNameSet = missionManager.getAwaitingMissionNameSet(player);
//        if (missionName.equals("all")) {
//            if (missionNameSet.isEmpty()) {
//                I18n.send(sender, "command.acquire.empty");
//                return;
//            }
//            missionNameSet.forEach(
//                    (mission) -> {
//                        if (missionManager.completeMission(player, mission)) {
//                            I18n.send(sender, "command.acquire.success", mission);
//                        } else {
//                            I18n.send(sender, "command.acquire.failed", mission);
//                        }
//                    }
//            );
//
//        } else {
//            if (!missionNameSet.contains(missionName)) {
//                I18n.send(sender, "command.acquire.not_found", missionName);
//            } else {
//                if (missionManager.completeMission(player, missionName)) {
//                    I18n.send(sender, "command.acquire.success", missionName);
//                } else {
//                    I18n.send(sender, "command.acquire.failed", missionName);
//                }
//            }
//        }
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
