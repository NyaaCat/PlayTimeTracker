package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.I18n;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/// sync task
public class DisplayPlayTimeTask implements ITask {

    public final Logger logger = LoggerUtils.getPluginLogger();

    private final @Nullable Context context;
    private final @Nullable UUID targetUUID;
    private final String targetName;
    private final CommandSender sender;
    private @Nullable TimeTrackerDbModel model;
    private @Nullable List<Pair<String, Duration>> records;
    private final DisplayNextMissionMode mode;

    public DisplayPlayTimeTask(String targetName, @Nullable TimeTrackerDbModel model, @Nullable List<Pair<String, Duration>> records, CommandSender sender, DisplayNextMissionMode mode) {
        this.mode = mode;
        this.context = null;
        this.targetUUID = null;
        this.targetName = targetName;
        this.model = model;
        this.records = records;
        this.sender = sender;
    }

    public DisplayPlayTimeTask(Context context, UUID targetUUID, String targetName, CommandSender sender) {
        this.context = context;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.mode = DisplayNextMissionMode.None;
        this.model = null;
        this.records = null;
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

        if (this.mode == DisplayNextMissionMode.First) {
            if (this.records == null || this.records.isEmpty()) {
                I18n.send(this.sender, "command.view.next_mission_empty");
            } else {
                var next = this.records.getFirst();
                var missionName = next.left();
                var duration = next.right().toMillis();
                I18n.send(this.sender, "command.view.next_mission_first", missionName, TimeUtils.timeFormat(duration));
            }
        } else if (this.mode == DisplayNextMissionMode.All) {
            if (this.records == null || this.records.isEmpty()) {
                I18n.send(this.sender, "command.view.next_mission_empty");
            } else {
                I18n.send(this.sender, "command.view.next_mission_title");
                for (var pair : this.records) {
                    var missionName = pair.left();
                    var duration = pair.right().toMillis();
                    I18n.send(this.sender, "command.view.next_mission_item", missionName, TimeUtils.timeFormat(duration));
                }
            }
        }
    }

    private void doQueryAsync() {
        assert this.context != null;
        this.model = this.context.getTimeTrackerConnection().getPlayerTimeTracker(this.targetUUID);
        this.context.getExecutor().sync(this);
    }
}
