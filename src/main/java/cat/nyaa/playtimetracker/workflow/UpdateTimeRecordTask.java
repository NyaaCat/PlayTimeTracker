package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/// async task (first step)
public class UpdateTimeRecordTask implements ITask {

    public final static Logger logger = LoggerUtils.getPluginLogger();

    public static final int OPTION_NONE = 0x0; // no option
    public static final int OPTION_ACCUMULATE = 0x1;
    public static final int OPTION_LAST_SEEN = 0x2;
    public static final int OPTION_CHECK_MISSIONS = 0x4; // wait for next update, used in UPDATE event

    private final Context context;
    private final UpdateWorkflow workflow;
    private final int option;

    public UpdateTimeRecordTask(Context context, UpdateWorkflow workflow, int option) {
        this.context = context;
        this.workflow = workflow;
        this.option = option;
        this.workflow.retain();
    }

    @Override
    public void execute(@Nullable Long tick) {
        if (tick != null) {
            // ensure step 0 is executed in async thread
            this.context.getExecutor().async(this);
            return;
        }

        try {
            logger.trace("UpdateTimeRecordTask execute Start player={}", this.workflow.getPlayerContext().getUUID());

            var model = updateTimeTrackerDbModel();
            this.workflow.setModel(model);

            if ((this.option & OPTION_CHECK_MISSIONS) == 0) {
                return;
            }

            var missionCfg = context.getMissionConfig();
            this.workflow.allocate(missionCfg.missions.size());
            for (var e : missionCfg.missions.entrySet()) {
                var checkMissionTask = new CheckMissionTask(this.context, this.workflow, e.getKey(), e.getValue());
                this.context.getExecutor().sync(checkMissionTask);
            }

            logger.trace("UpdateTimeRecordTask execute End");
        } finally {
            this.workflow.release(tick);
        }
    }

    private TimeTrackerDbModel updateTimeTrackerDbModel() {
        var playerContext = this.workflow.getPlayerContext();
        var time = this.workflow.getTime();
        var timeTrackerConnection = this.context.getTimeTrackerConnection();
        var model = timeTrackerConnection.getPlayerTimeTracker(playerContext.getUUID());
        if (model == null) {
            model = new TimeTrackerDbModel(playerContext.getUUID(), time.getTimestamp(), 0, 0, 0, 0, time.getTimestamp());
            timeTrackerConnection.insertPlayer(model);
            logger.trace("UpdateTimeRecordTask create new player record");
        } else {
            var legacy = model.getLastUpdate();
            UpdateTimeRecordTask.updateTimeTrackerDbModel(model, time, (this.option & OPTION_ACCUMULATE) != 0, (this.option & OPTION_LAST_SEEN) != 0);
            timeTrackerConnection.updateDbModel(model);
            logger.trace("UpdateTimeRecordTask update player record {} => {}: {}", legacy, time.getTimestamp(), model);
        }
        return model;
    }

    public static void updateTimeTrackerDbModel(TimeTrackerDbModel model, PiecewiseTimeInfo time, boolean accumulate, boolean lastSeen) {
        if (accumulate) {
            var duration = time.getTimestamp() - model.getLastUpdate();
            if (time.getSameDayStart() <= model.getLastUpdate()) {
                model.addDailyTime(duration);
            } else {
                model.setDailyTime(time.getTimestamp() - time.getSameDayStart());
            }
            if (time.getSameWeekStart() <= model.getLastUpdate()) {
                model.addWeeklyTime(duration);
            } else {
                model.setWeeklyTime(time.getTimestamp() - time.getSameWeekStart());
            }
            if (time.getSameMonthStart() <= model.getLastUpdate()) {
                model.addMonthlyTime(duration);
            } else {
                model.setMonthlyTime(time.getTimestamp() - time.getSameMonthStart());
            }
            model.addTotalTime(duration);
        } else {
            if (time.getSameDayStart() > model.getLastUpdate()) {
                model.setDailyTime(0);
            }
            if (time.getSameWeekStart() > model.getLastUpdate()) {
                model.setWeeklyTime(0);
            }
            if (time.getSameMonthStart() > model.getLastUpdate()) {
                model.setMonthlyTime(0);
            }
        }
        if (lastSeen) {
            model.setLastSeen(time.getTimestamp());
        }
        model.setLastUpdate(time.getTimestamp());
    }
}
