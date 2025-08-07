package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

/// sync task
public class DisplayPlayTimeTask implements ITask {

    public final Logger logger = LoggerUtils.getPluginLogger();

    private final @Nullable Context context;
    private final @Nullable UUID targetUUID;
    private final String targetName;
    private final CommandSender sender;
    private @Nullable TimeTrackerDbModel model;

    public DisplayPlayTimeTask(String targetName, TimeTrackerDbModel model, CommandSender sender) {
        this.context = null;
        this.targetUUID = null;
        this.targetName = targetName;
        this.model = model;
        this.sender = sender;
    }

    public DisplayPlayTimeTask(Context context, UUID targetUUID, String targetName, CommandSender sender) {
        this.context = context;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.model = null;
        this.sender = sender;
    }
    
    @Override
    public void execute(@Nullable Long tick) {
        logger.trace("DisplayPlayTimeTask execute START; targetName={}", this.targetName);
        if (tick != null) {
            doDisplaySync(tick);
        } else if (this.context != null) {
            doQueryAsync();
        }
        logger.trace("DisplayPlayTimeTask execute END");
    }

    private void doDisplaySync(long tick) {
        if (this.sender instanceof OfflinePlayer player) {
            if (!player.isOnline()) {
                logger.warn("DisplayPlayTimeTask execute: player {} is offline, cannot display playtime", player.getUniqueId());
                return;
            }
        }
        if (this.model == null) {
            I18n.send(this.sender, "command.view.no_record", this.targetName);
            return;
        }
        I18n.send(this.sender, "command.view.query_title", this.targetName, this.model.getPlayerUniqueId());
        I18n.send(this.sender, "command.view.last_update", TimeUtils.dateFormat(this.model.getLastUpdate()));
        I18n.send(this.sender, "command.view.daily_time", TimeUtils.timeFormat(this.model.getDailyTime()));
        I18n.send(this.sender, "command.view.weekly_time", TimeUtils.timeFormat(this.model.getWeeklyTime()));
        I18n.send(this.sender, "command.view.monthly_time", TimeUtils.timeFormat(this.model.getMonthlyTime()));
        I18n.send(this.sender, "command.view.total_time", TimeUtils.timeFormat(this.model.getTotalTime()));
        I18n.send(this.sender, "command.view.last_seen", TimeUtils.dateFormat(this.model.getLastSeen()));
    }

    private void doQueryAsync() {
        assert this.context != null;
        this.model = this.context.getTimeTrackerConnection().getPlayerTimeTracker(this.targetUUID);
        this.context.getExecutor().sync(this);
    }
}
