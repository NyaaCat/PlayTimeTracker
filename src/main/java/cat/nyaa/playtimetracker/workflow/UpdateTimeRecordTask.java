package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import org.bukkit.entity.Player;

public class UpdateTimeRecordTask implements ITask {

    private final Player player;
    private final PiecewiseTimeInfo time;
    private final TriggerEvent event;

    public UpdateTimeRecordTask(final Player player, final PiecewiseTimeInfo time, TriggerEvent event) {
        this.player = player;
        this.time = time;
        this.event = event;
    }

    @Override
    public int execute(Workflow workflow, boolean executeInGameLoop) throws Exception {
        if (executeInGameLoop) {
            return -1;
        }
        var timeTrackerConnection = workflow.getTimeTrackerConnection();
        var playerUniqueId = player.getUniqueId();
        var model = timeTrackerConnection.getPlayerTimeTracker(playerUniqueId);
        if (model == null) {
            model = new TimeTrackerDbModel(playerUniqueId, this.time.getTimestamp(), 0, 0, 0, 0, this.time.getTimestamp());
            timeTrackerConnection.insertPlayer(model);
        } else {
            var duration = this.time.getTimestamp() - model.getLastUpdate();
            if (this.time.getSameDayStart() <= model.getLastUpdate()) {
                model.addDailyTime(duration);
            } else {
                model.setDailyTime(this.time.getTimestamp() - this.time.getSameDayStart());
            }
            if (this.time.getSameWeekStart() <= model.getLastUpdate()) {
                model.addWeeklyTime(duration);
            } else {
                model.setWeeklyTime(this.time.getTimestamp() - this.time.getSameWeekStart());
            }
            if (this.time.getSameMonthStart() <= model.getLastUpdate()) {
                model.addMonthlyTime(duration);
            } else {
                model.setMonthlyTime(this.time.getTimestamp() - this.time.getSameMonthStart());
            }
            if (this.event.source() == TriggerEvent.TriggerType.LOGOUT || this.event.source() == TriggerEvent.TriggerType.DISABLE) {
                model.setLastSeen(this.time.getTimestamp());
            }
            model.setLastUpdate(this.time.getTimestamp());
            timeTrackerConnection.updateDbModel(model);
        }

        var missionCfg = workflow.getMissionConfig();
        var cachedPlayerData = new CheckMissionTask.PlayerRelatedCache(this.player, workflow.getEssentialsPlugin());
        var nextWaitCollect = (CheckMissionTask.IWaitTimeCollection) null;
        for (var e : missionCfg.missions.entrySet()) {
            var check = new CheckMissionTask(cachedPlayerData, e.getKey(), e.getValue(), this.time, model, nextWaitCollect);
            workflow.addNextWorkStep(check, true);
        }

        return 0;
    }
}
