package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.nyaacore.orm.WhereClause;
import cat.nyaa.nyaacore.orm.backends.IConnectedDatabase;
import cat.nyaa.nyaacore.orm.backends.ITypedTable;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record TimeTrackerConnection(
        ITypedTable<TimeTrackerDbModel> timeTrackerTable, IConnectedDatabase db) {
    public void deletePlayerData(UUID playerId) {
        synchronized (TimeTrackerConnection.class) {
            TimeTrackerDbModel trackerDbModel = getPlayerTimeTracker(playerId);
            if (trackerDbModel == null) return;
            timeTrackerTable.delete(WhereClause.EQ("player", playerId));
        }
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
                timeTrackerTable.insert(trackerDbModel);
            }
//            else {
//                trackerDbModel.setLastSeen(time);
//                timeTrackerTable.update(trackerDbModel, WhereClause.EQ("player", playerId), "lastSeen");
//            }
        }
    }

    @Nullable
    public TimeTrackerDbModel getPlayerTimeTracker(UUID playerId) {
        return timeTrackerTable.selectUniqueUnchecked(WhereClause.EQ("player", playerId));
    }

    public void updateDbModel(UUID playerId, TimeTrackerDbModel model) {
        timeTrackerTable.update(model, WhereClause.EQ("player", playerId));
    }
}
