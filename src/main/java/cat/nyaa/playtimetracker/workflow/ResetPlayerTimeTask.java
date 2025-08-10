package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.executor.ITask;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ResetPlayerTimeTask implements ITask {

    private final Context context;
    private final CommandSender sender;
    private final UUID playerUUID;
    private final @Nullable String playerName;
    private final @Nullable ITask postTaskSync;
    private final @Nullable ITask postTaskAsync;
    private boolean success;

    public ResetPlayerTimeTask(Context context, CommandSender sender, UUID playerUUID, @Nullable String playerName, @Nullable ITask postTaskSync, @Nullable ITask postTaskAsync) {
        this.context = context;
        this.sender = sender;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.postTaskSync = postTaskSync;
        this.postTaskAsync = postTaskAsync;
        this.success = false;
    }

    @Override
    public void execute(@Nullable Long tick) {
        if (tick == null) {
            doResetAsync();
            if (this.success && this.postTaskAsync != null) {
                this.postTaskAsync.execute(null);
            }
        } else {
            doNotifySync(tick);
            if (this.success && this.postTaskSync != null) {
                this.postTaskSync.execute(tick);
            }
        }
    }

    private void doResetAsync() {
        var conn = this.context.getTimeTrackerConnection();
        var data = conn.getPlayerTimeTracker(this.playerUUID);
        if (data == null) {
            this.success = false;
        } else {
            conn.deletePlayerData(this.playerUUID);
            this.success = true;
        }
        this.context.getExecutor().sync(this);

    }

    private void doNotifySync(long tick) {
        if (this.sender instanceof Player player) {
            if (!player.isOnline()) {
                return;
            }
        }
        if (this.success) {
            I18n.send(this.sender, "command.reset.time.success", this.playerName == null ? this.playerUUID.toString() : this.playerName);
        } else {
            I18n.send(this.sender, "command.reset.time.not_found", this.playerName == null ? this.playerUUID.toString() : this.playerName);
        }
    }
}
