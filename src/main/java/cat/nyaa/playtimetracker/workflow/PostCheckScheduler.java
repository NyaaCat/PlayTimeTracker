package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.executor.AbstractFinalTrigger;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class PostCheckScheduler extends AbstractFinalTrigger implements CheckMissionTask.IScheduler, ITask {

    private static final Logger logger = LoggerUtils.getPluginLogger();

    private final Context context;
    private final PlayerContext playerContext;
    private final Function<String, ITask> factoryNextUpdate; // Factory to create the next update task, with the mission name as input
    private final boolean enableReplyWait;
    private final AtomicInteger index;
    private final Pair<String, Duration>[] records;
    private List<Pair<String, Duration>> recordList;

    public PostCheckScheduler(int count, Context context, PlayerContext playerContext, Function<String, ITask> factoryNextUpdate, boolean enableReplyWait) {
        this.context = context;
        this.playerContext = playerContext;
        this.factoryNextUpdate = factoryNextUpdate;
        this.enableReplyWait = enableReplyWait;
        this.index = new AtomicInteger(0);
        this.records = new Pair[count];
        this.recordList = null;
    }

    @Override
    public void record(String mission, Duration waitTime) {
        int index = this.index.getAndIncrement();
        this.records[index] = Pair.of(mission, waitTime);
    }

    @Override
    protected void handle() {
        int n = this.index.get();
        this.recordList = ObjectArrayList.wrap(this.records, n);
        this.context.getExecutor().async(this);
    }

    @Override
    public void execute(@Nullable Long tick) {
        if (this.recordList == null) {
            return;
        }
        this.recordList.sort(PostCheckScheduler::compareRecord);
        if (this.enableReplyWait) {
            // TODO: reply wait time
            //ITask replyMissionTask = this.factoryReplyMissionTask.apply(this.playerContext, this.recordList);
            //this.context.getExecutor().sync(replyMissionTask);
        }
        if (!this.recordList.isEmpty()) {
            var record = this.recordList.getFirst();
            var mission = record.first();
            var waitTime = record.second();
            ITask task = this.factoryNextUpdate.apply(mission);
            var success = this.context.scheduleWorkflowTask(waitTime, this.playerContext.getUUID(), task);
            if (success) {
                logger.info("WorkflowScheduler add task for player {} with record @{} for {}", this.playerContext.getUUID(), record.first(), waitTime);
            }
        }
    }

    private static int compareRecord(Pair<String, Duration> r1, Pair<String, Duration> r2) {
        return r1.second().compareTo(r2.second());
    }
}
