package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.executor.ITask;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class UpdateWorkflow {

    private final PlayerContext playerContext;
    private final PiecewiseTimeInfo time;
    private @Nullable TimeTrackerDbModel model;
    private AtomicInteger index;
    private Pair<String, Duration>[] records;
    private List<Pair<String, Duration>> recordList;
    private final AtomicInteger refCnt;
    private final @Nullable BiConsumer<@Nullable Long, UpdateWorkflow> callback;

    public UpdateWorkflow(PlayerContext playerContext, PiecewiseTimeInfo time, @Nullable BiConsumer<@Nullable Long, UpdateWorkflow> callback) {
        this.playerContext = playerContext;
        this.time = time;
        this.index = null;
        this.records = null;
        this.recordList = null;
        this.refCnt = new AtomicInteger(0);
        this.callback = callback;
    }

    public PlayerContext getPlayerContext() {
        return this.playerContext;
    }

    public PiecewiseTimeInfo getTime() {
        return this.time;
    }

    public @Nullable TimeTrackerDbModel getModel() {
        return this.model;
    }

    public void setModel(@Nullable TimeTrackerDbModel model) {
        this.model = model;
    }

    public void retain() {
        if(this.refCnt.getAndIncrement() < 0) {
            this.refCnt.set(-1);
            throw new IllegalStateException("retain on released context");
        }
    }

    public void release(@Nullable Long tick) {
        if (this.refCnt.decrementAndGet() == 0) {
            this.refCnt.set(-1);
            var cb = this.callback;
            if (cb != null) {
                cb.accept(tick, this);
            }
        }
    }

    public void allocate(int count) {
        if (this.index != null) {
            throw new IllegalStateException("already allocated");
        }
        this.index = new AtomicInteger(0);
        //noinspection unchecked
        this.records = new Pair[count];
    }

    public void record(String mission, Duration wait) {
        if (this.index == null || this.records == null) {
            throw new IllegalStateException("not allocated");
        }
        int index = this.index.getAndIncrement();
        if (index >= this.records.length) {
            throw new IndexOutOfBoundsException("record overflow");
        }
        this.records[index] = Pair.of(mission, wait);
    }

    public List<Pair<String, Duration>> getRecordList() {
        if (this.recordList == null) {
            int n = this.index.get();
            this.recordList = ObjectArrayList.wrap(this.records, n);
            this.recordList.sort(UpdateWorkflow::compareRecord);
        }
        return this.recordList;
    }

    private static int compareRecord(Pair<String, Duration> r1, Pair<String, Duration> r2) {
        return r1.second().compareTo(r2.second());
    }
}
