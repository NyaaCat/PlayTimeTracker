package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.nyaacore.orm.backends.BackendConfig;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.db.tables.TimeTrackerTable;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class TimeTrackerConnection {
   // static ConcurrentHashMap<UUID,TimeTrackerDbModel> cache = new ConcurrentHashMap<>();
    private final TimeTrackerTable timeTrackerTable;
    private final HikariDataSource ds;
//    private final AsyncDbManager<TimeTrackerDbModel> asyncDbManager;

    public TimeTrackerConnection(HikariDataSource ds, Plugin plugin, BackendConfig backendConfig) {
        this.ds = ds;
//        this.asyncDbManager = AsyncDbManager.create(TimeTrackerDbModel.class, plugin, backendConfig);
        this.timeTrackerTable = new TimeTrackerTable(ds);
    }

    public void deletePlayerData(UUID playerId) {
        synchronized (TimeTrackerConnection.class) {
            TimeTrackerDbModel trackerDbModel = getPlayerTimeTracker(playerId);
            if (trackerDbModel == null) return;
//            cache.remove(trackerDbModel.getPlayerUniqueId());
            timeTrackerTable.deletePlayer(playerId);
        }
    }

    public void insertPlayer(TimeTrackerDbModel trackerDbModel) {
        timeTrackerTable.insertPlayer(trackerDbModel);
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
//        if(cache.containsKey(playerId)) {
//            return cache.get(playerId);
//        }
        return getPlayerTimeTrackerNocache(playerId);
    }

    @Nullable
    public TimeTrackerDbModel getPlayerTimeTrackerNocache(UUID playerId) {

        return timeTrackerTable.selectPlayer(playerId);
    }


    public void updateDbModel(@NotNull TimeTrackerDbModel model) {
        if (getPlayerTimeTracker(model.getPlayerUniqueId()) == null)
            insertPlayer(model.getPlayerUniqueId(), TimeUtils.getUnixTimeStampNow());
//        cache.put(model.getPlayerUniqueId(),model);
        timeTrackerTable.update(model, model.getPlayerUniqueId());
    }

    public void doAsyncUpdate() {
//        asyncDbManager.saveModel(cache.values(), true);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TimeTrackerConnection) obj;
        return Objects.equals(this.timeTrackerTable, that.timeTrackerTable) &&
                Objects.equals(this.ds, that.ds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeTrackerTable, ds);
    }

    @Override
    public String toString() {
        return "TimeTrackerConnection[" +
                "timeTrackerTable=" + timeTrackerTable + ", " +
                "db=" + ds + ']';
    }

    public void close() {
//        asyncDbManager.saveModel(cache.values(), false);
//        this.asyncDbManager.close();
    }

}
