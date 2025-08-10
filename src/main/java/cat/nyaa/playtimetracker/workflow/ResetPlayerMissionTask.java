package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.executor.ITask;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ResetPlayerMissionTask implements ITask {

    private final Context context;
    private final CommandSender sender;
    private final UUID playerUUID;
    private final @Nullable String playerName;
    private final @Nullable String missionName;
    private final @Nullable ITask postTaskSync;
    private final @Nullable ITask postTaskAsync;
    private int success;

    public ResetPlayerMissionTask(Context context, CommandSender sender, UUID playerUUID, @Nullable String playerName, @Nullable String missionName, @Nullable ITask postTaskSync, @Nullable ITask postTaskAsync) {
        this.context = context;
        this.sender = sender;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.missionName = missionName;
        this.postTaskSync = postTaskSync;
        this.postTaskAsync = postTaskAsync;
        this.success = -1;  // -1: failed, 0: not found, 1 ~ N: found count
    }

    @Override
    public void execute(@Nullable Long tick) {
        if (tick == null) {
            doResetAsync();
            if (this.success > 0 && this.postTaskAsync != null) {
                this.postTaskAsync.execute(null);
            }
        } else {
            doNotifySync(tick);
            if (this.success > 0 && this.postTaskSync != null) {
                this.postTaskSync.execute(tick);
            }
        }
    }

    private void doResetAsync() {
        var conn = this.context.getCompletedMissionConnection();
        if (this.missionName == null) {
            var dataList = conn.getPlayerCompletedMissionList(this.playerUUID);
            conn.resetPlayerCompletedMission(this.playerUUID);
            this.success = dataList.size();
        } else {
            var data = conn.getPlayerCompletedMission(this.playerUUID, this.missionName);
            if (data == null) {
                this.success = 0; // not found
            } else {
                conn.resetPlayerCompletedMission(this.missionName, this.playerUUID);
                this.success = 1; // found
            }
        }
        this.context.getExecutor().sync(this);
    }

    private void doNotifySync(long tick) {
        if (this.sender instanceof Player player) {
            if (!player.isOnline()) {
                return;
            }
        }
        if (this.success < 0) {
            I18n.send(this.sender, "command.reset.mission.not_found", this.playerName == null ? this.playerUUID : this.playerName);
        } else if (this.success == 0) {
            I18n.send(this.sender, "command.reset.mission.mission_not_found", this.playerName == null ? this.playerUUID : this.playerName, this.missionName == null ? "" : this.missionName);
        } else {
            if (this.missionName == null) {
                I18n.send(this.sender, "command.reset.mission.success_all", this.playerName == null ? this.playerUUID : this.playerName);
            } else {
                I18n.send(this.sender, "command.reset.mission.success", this.playerName == null ? this.playerUUID : this.playerName, this.missionName);
            }
        }
    }
}
