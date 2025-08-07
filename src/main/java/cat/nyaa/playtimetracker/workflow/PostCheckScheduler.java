package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.executor.AbstractFinalTrigger;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.executor.ITaskExecutor;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PostCheckScheduler extends AbstractFinalTrigger implements CheckMissionTask.IScheduler, ITask {

    private final ITaskExecutor executor;
    private final IPostCheckCallback callback;
    private final AtomicInteger index;
    private final Pair<String, Duration>[] records;
    private List<Pair<String, Duration>> recordList;

    /// @param callback callback to handle the sorted records of missions and their wait times
    public PostCheckScheduler(int count, ITaskExecutor executor, IPostCheckCallback callback) {
        this.executor = executor;
        this.callback = callback;
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
    protected void handle(@Nullable Long tick) {
        int n = this.index.get();
        this.recordList = ObjectArrayList.wrap(this.records, n);
        if (tick == null) {
            this.execute(null);
        } else {
            this.executor.async(this);
        }
    }

    @Override
    public void execute(@Nullable Long tick) {
        if (this.recordList == null) {
            return;
        }
        this.recordList.sort(PostCheckScheduler::compareRecord);
        this.callback.handle(null, null, this.recordList);
    }

    private static int compareRecord(Pair<String, Duration> r1, Pair<String, Duration> r2) {
        return r1.second().compareTo(r2.second());
    }
}
