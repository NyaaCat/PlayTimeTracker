package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.nyaacore.orm.WhereClause;
import cat.nyaa.nyaacore.orm.backends.BackendConfig;
import cat.nyaa.nyaacore.orm.backends.IConnectedDatabase;
import cat.nyaa.nyaacore.orm.backends.ITypedTable;
import cat.nyaa.playtimetracker.db.async.AsyncDbManager;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public final class TimeTrackerConnection {
    static CopyOnWriteArraySet<TimeTrackerDbModel> cache = new CopyOnWriteArraySet<>();
    private final ITypedTable<TimeTrackerDbModel> timeTrackerTable;
    private final IConnectedDatabase db;
    private final AsyncDbManager<TimeTrackerDbModel> asyncDbManager;

    public TimeTrackerConnection(ITypedTable<TimeTrackerDbModel> timeTrackerTable, IConnectedDatabase db, Plugin plugin, BackendConfig backendConfig) {
        this.timeTrackerTable = timeTrackerTable;
        this.db = db;
        this.asyncDbManager = AsyncDbManager.create(TimeTrackerDbModel.class, plugin, backendConfig);
    }

    public void deletePlayerData(UUID playerId) {
        synchronized (TimeTrackerConnection.class) {
            TimeTrackerDbModel trackerDbModel = getPlayerTimeTracker(playerId);
            if (trackerDbModel == null) return;
            cache.remove(trackerDbModel);
            timeTrackerTable.delete(WhereClause.EQ("player", playerId));
        }
    }
    public void insertPlayer(TimeTrackerDbModel trackerDbModel) {
        timeTrackerTable.insert(trackerDbModel);
    }
    public void insertPlayer(UUID playerId, long time) {
        synchronized (TimeTrackerConnection.class) {
            TimeTrackerDbModel trackerDbModel = getPlayerTimeTracker(playerId);
            if (trackerDbModel == null) {
                trackerDbModel = new TimeTrackerDbModel();
                trackerDbModel.setPlayerUniqueId(playerId);
                trackerDbModel.setDailyTime(0L);
                trackerDbModel.setWeeklyTime(0L);
                trackerDbModel.setMonthlyTime(0L);
                trackerDbModel.setTotalTime(0L);
                trackerDbModel.setLastSeen(time);
                insertPlayer(trackerDbModel);
            }
//            else {
//                trackerDbModel.setLastSeen(time);
//                timeTrackerTable.update(trackerDbModel, WhereClause.EQ("player", playerId), "lastSeen");
//            }
        }
    }

    @Nullable
    public TimeTrackerDbModel getPlayerTimeTracker(UUID playerId) {
        for (TimeTrackerDbModel trackerDbModel : Collections.unmodifiableCollection(cache)) {
            if (trackerDbModel.getPlayerUniqueId() == playerId) {
                return trackerDbModel;
            }
        }
        return getPlayerTimeTrackerNocache(playerId);
    }

    @Nullable
    public TimeTrackerDbModel getPlayerTimeTrackerNocache(UUID playerId) {

        return timeTrackerTable.selectUniqueUnchecked(WhereClause.EQ("player", playerId));
    }


    public void updateDbModel(TimeTrackerDbModel model) {
        cache.add(model);
        //timeTrackerTable.update(model, WhereClause.EQ("player", model.getPlayerUniqueId()));
    }

    public void doAsyncUpdate() {
        asyncDbManager.saveModel(cache, true);
    }

    public ITypedTable<TimeTrackerDbModel> timeTrackerTable() {
        return timeTrackerTable;
    }

    public IConnectedDatabase db() {
        return db;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TimeTrackerConnection) obj;
        return Objects.equals(this.timeTrackerTable, that.timeTrackerTable) &&
                Objects.equals(this.db, that.db);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeTrackerTable, db);
    }

    @Override
    public String toString() {
        return "TimeTrackerConnection[" +
                "timeTrackerTable=" + timeTrackerTable + ", " +
                "db=" + db + ']';
    }

    public void close() {
        asyncDbManager.saveModel(cache, false);
        this.asyncDbManager.close();
    }
}
