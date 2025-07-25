package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class UpdateTimeRecordTask implements ITask {

    public final static Logger logger = LoggerUtils.getPluginLogger();

    private final Context context;
    private final PlayerContext playerContext;
    private PiecewiseTimeInfo time;
    private final PiecewiseTimeInfo.Builder timeBuilder;
    private final TriggerEvent event;
    private final @Nullable String mission;

    public UpdateTimeRecordTask(Context context, PlayerContext playerContext, PiecewiseTimeInfo time, TriggerEvent event, @Nullable String mission) {
        this.context = context;
        this.playerContext = playerContext;
        this.time = time;
        this.timeBuilder = null; // time is already provided
        this.event = event;
        this.mission = mission;
    }

    private UpdateTimeRecordTask(Context context, PlayerContext playerContext, PiecewiseTimeInfo.Builder timeBuilder, final TriggerEvent event, @Nullable String mission) {
        this.context = context;
        this.playerContext = playerContext;
        this.timeBuilder = timeBuilder;
        this.mission = mission;
        this.time = null;
        this.event = event;
    }

    @Override
    public void execute(Long tick) {
        logger.trace("UpdateTimeRecordTask execute@{} Start player={},event={},{}", tick, this.playerContext.getUUID(), this.event, this.mission);

        if (this.time == null) {
            var current = TimeUtils.getInstantNow();
            assert this.timeBuilder != null;
            this.time = this.timeBuilder.build(current);
        }
        var model = updateTimeTrackerDbModel();

        if (this.event.isEndEvent()) {
            // If the event is DISABLE or LOGOUT, we need not check missions
            return;
        }

        var missionCfg = context.getMissionConfig();
        var tracker = new LimitedTimeTrackerModel(model, this.time);

        // TODO: judge whether to reply or not
        var reply = false;

        var finalTrigger = new PostCheckScheduler(missionCfg.missions.size(), this.context, this.playerContext, this::createNextUpdateTask, reply);
        for (var e : missionCfg.missions.entrySet()) {
            var checkMissionTask = new CheckMissionTask(this.context, this.playerContext, tracker, e.getKey(), e.getValue(), finalTrigger);
            this.context.getExecutor().sync(checkMissionTask);
        }

        logger.trace("UpdateTimeRecordTask execute End");
    }

    private TimeTrackerDbModel updateTimeTrackerDbModel() {
        var timeTrackerConnection = this.context.getTimeTrackerConnection();
        var model = timeTrackerConnection.getPlayerTimeTracker(this.playerContext.getUUID());
        if (model == null) {
            model = new TimeTrackerDbModel(this.playerContext.getUUID(), this.time.getTimestamp(), 0, 0, 0, 0, this.time.getTimestamp());
            timeTrackerConnection.insertPlayer(model);
            logger.trace("UpdateTimeRecordTask create new player record");
        } else {
            var duration = this.event.isBeginEvent() ? 0 : this.time.getTimestamp() - model.getLastUpdate();
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
            if (this.event == TriggerEvent.LOGOUT) {
                model.setLastSeen(this.time.getTimestamp());
            }
            model.addTotalTime(duration);
            model.setLastUpdate(this.time.getTimestamp());
            timeTrackerConnection.updateDbModel(model);
            logger.trace("UpdateTimeRecordTask update player record for {}ms: {}", duration, model);
        }
        return model;
    }

    ITask createNextUpdateTask(@Nullable String mission) {
        return new UpdateTimeRecordTask(this.context, this.playerContext, PiecewiseTimeInfo.Builder.extract(this.time), this.event, this.mission);
    }
}
