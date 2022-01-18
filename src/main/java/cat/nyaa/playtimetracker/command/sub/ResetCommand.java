package cat.nyaa.playtimetracker.command.sub;

import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.Utils.CommandUtils;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public class ResetCommand extends CommandReceiver {

    /**
     * @param plugin for logging purpose only
     * @param _i18n  i18n
     */
    public ResetCommand(Plugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }

    @SubCommand(value = "mission", permission = "ptt.command.reset.mission")
    public void resetMission(CommandSender sender, Arguments args) {
        if (PlayTimeTracker.getInstance() == null || PlayTimeTracker.getInstance().getMissionManager() == null) {
            I18n.send(sender, "command.reset.mission.err");
            return;
        }
        String playerName = args.nextString();
        String missionName = args.nextString();
        UUID playerId = CommandUtils.getPlayerUUIDByStr(playerName, sender);
        if (playerId == null) {
            I18n.send(sender, "command.reset.mission.target_not_found", playerName);
            return;
        }
        List<CompletedMissionDbModel> allData = PlayTimeTracker.getInstance().getMissionManager().getPlayerCompletedMissionList(playerId);
        if (allData == null || allData.isEmpty()) {
            I18n.send(sender, "command.reset.mission.not_found", playerName);
            return;
        }
        if (missionName.isEmpty()) {
            I18n.send(sender, "command.reset.mission.mission_not_found", playerName, "");
            return;
        }
        if (missionName.equals("all")) {
            PlayTimeTracker.getInstance().getMissionManager().resetPlayerMissionData(playerId);
            I18n.send(sender, "command.reset.mission.success_all",playerName);
        } else {
            if (!PlayTimeTracker.getInstance().getMissionManager().getMissionDataMap().containsKey(missionName)) {
                I18n.send(sender, "command.reset.mission.mission_not_found", playerName, missionName);
                return;
            }
            boolean canReset = false;
            for (CompletedMissionDbModel missionDbModel : allData) {
                if (missionDbModel.getMissionName().equals(missionName)) {
                    canReset = true;
                    break;
                }
            }
            if (!canReset) {
                I18n.send(sender, "command.reset.mission.mission_not_found", playerName, missionName);
                return;
            }
            PlayTimeTracker.getInstance().getMissionManager().resetPlayerMissionData(playerId);
            I18n.send(sender, "command.reset.mission.success",playerName,missionName);
        }


    }

    @SubCommand(value = "time", permission = "ptt.command.reset.time")
    public void resetTime(CommandSender sender, Arguments args) {
        if (PlayTimeTracker.getInstance() == null || PlayTimeTracker.getInstance().getTimeRecordManager() == null) {
            I18n.send(sender, "command.reset.time.err");
            return;
        }
        String playerName = args.nextString();
        UUID playerId = CommandUtils.getPlayerUUIDByStr(args.nextString(), sender);
        if (playerId == null) {
            I18n.send(sender, "command.reset.time.target_not_found", playerName);
            return;
        }
        if (PlayTimeTracker.getInstance().getTimeRecordManager().getPlayerTimeTrackerDbModel(playerId) == null) {
            I18n.send(sender, "command.reset.time.not_found", playerName);
            return;
        }
        PlayTimeTracker.getInstance().getTimeRecordManager().resetPlayerRecordData(playerId);
        I18n.send(sender, "command.reset.time.success", playerName);
    }

    @Override
    public String getHelpPrefix() {
        return "reset";
    }
}
