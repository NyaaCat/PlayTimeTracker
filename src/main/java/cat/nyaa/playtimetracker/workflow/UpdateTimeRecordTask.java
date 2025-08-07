package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;

/// async task (first step)
public class UpdateTimeRecordTask implements ITask, IPostCheckCallback {

    public final static Logger logger = LoggerUtils.getPluginLogger();

    public static final int OPTION_NONE = 0x0; // no option
    public static final int OPTION_ACCUMULATE = 0x1;
    public static final int OPTION_LAST_SEEN = 0x2;
    public static final int OPTION_WAIT_NEXT = 0x4; // wait for next update, used in UPDATE event

    private final Context context;
    private final PlayerContext playerContext;
    private PiecewiseTimeInfo time;
    private final PiecewiseTimeInfo.Builder timeBuilder;
    private final int option;
    private final @Nullable IPostCheckCallback[] callbacks; // null if no callbacks are needed, otherwise length > 0
    private TimeTrackerDbModel model;

    public UpdateTimeRecordTask(Context context, PlayerContext playerContext, PiecewiseTimeInfo time, int option) {
        this.context = context;
        this.playerContext = playerContext;
        this.time = time;
        this.option = option;
        this.timeBuilder = null; // time is already provided
        this.callbacks = null;
        this.model = null;
    }

    public UpdateTimeRecordTask(Context context, PlayerContext playerContext, PiecewiseTimeInfo time, int option, IPostCheckCallback... callbacks) {
        this.context = context;
        this.playerContext = playerContext;
        this.time = time;
        this.option = option;
        this.timeBuilder = null; // time is already provided
        this.callbacks = shrink(callbacks);
        this.model = null;
    }

    private UpdateTimeRecordTask(Context context, PlayerContext playerContext, PiecewiseTimeInfo.Builder timeBuilder, int option, IPostCheckCallback... callbacks) {
        this.context = context;
        this.playerContext = playerContext;
        this.option = option;
        this.time = null;
        this.timeBuilder = timeBuilder;
        this.callbacks = shrink(callbacks);
        this.model = null;
    }

    @Override
    public void execute(@Nullable Long tick) {
        logger.trace("UpdateTimeRecordTask execute Start player={}", this.playerContext.getUUID());

        if (this.time == null) {
            var current = TimeUtils.getInstantNow();
            assert this.timeBuilder != null;
            this.time = this.timeBuilder.build(current);
        }
        this.model = updateTimeTrackerDbModel();

        if ((this.option & OPTION_WAIT_NEXT) == 0 && this.callbacks == null) {
            return;
        }

        var missionCfg = context.getMissionConfig();
        var tracker = new LimitedTimeTrackerModel(this.model, this.time);

        // TODO: judge whether to reply or not
        var finalTrigger = new PostCheckScheduler(missionCfg.missions.size(), this.context.getExecutor(), this);
        for (var e : missionCfg.missions.entrySet()) {
            var checkMissionTask = new CheckMissionTask(this.context, this.playerContext, tracker, e.getKey(), e.getValue(), finalTrigger);
            this.context.getExecutor().sync(checkMissionTask);
        }

        logger.trace("UpdateTimeRecordTask execute End");
    }

    @Override
    public void handle(PlayerContext _playerContext, TimeTrackerDbModel _model, List<Pair<String, Duration>> records) {
        if ((this.option & OPTION_WAIT_NEXT) != 0 && !records.isEmpty()) {
            var record = records.getFirst();
            var mission = record.first();
            var waitTime = record.second();
            assert this.time != null;
            ITask task = new UpdateTimeRecordTask(this.context, this.playerContext, PiecewiseTimeInfo.Builder.extract(this.time), OPTION_ACCUMULATE | OPTION_WAIT_NEXT);
            var success = this.context.scheduleWorkflowTask(waitTime, this.playerContext.getUUID(), task);
            if (success) {
                logger.info("UpdateTimeRecordTask-PostCheckScheduler add task for player {} with record @{} for {}", this.playerContext.getUUID(), record.first(), waitTime);
            }
        }
        if (this.callbacks != null) {
            for (var callback : this.callbacks) {
                if (callback != null) {
                    callback.handle(this.playerContext, this.model, records);
                }
            }
        }
    }

    private TimeTrackerDbModel updateTimeTrackerDbModel() {
        var timeTrackerConnection = this.context.getTimeTrackerConnection();
        var model = timeTrackerConnection.getPlayerTimeTracker(this.playerContext.getUUID());
        if (model == null) {
            model = new TimeTrackerDbModel(this.playerContext.getUUID(), this.time.getTimestamp(), 0, 0, 0, 0, this.time.getTimestamp());
            timeTrackerConnection.insertPlayer(model);
            logger.trace("UpdateTimeRecordTask create new player record");
        } else {
            var legacy = model.getLastUpdate();
            UpdateTimeRecordTask.updateTimeTrackerDbModel(model, this.time, (this.option & OPTION_ACCUMULATE) != 0, (this.option & OPTION_LAST_SEEN) != 0);
            timeTrackerConnection.updateDbModel(model);
            logger.trace("UpdateTimeRecordTask update player record {} => {}: {}", legacy, this.time.getTimestamp(), model);
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

    private static @Nullable IPostCheckCallback[] shrink(@Nullable IPostCheckCallback[] callbacks) {
        if (callbacks == null) {
            return null;
        }
        if (callbacks.length == 0) {
            return null;
        }
        return callbacks;
    }
}
