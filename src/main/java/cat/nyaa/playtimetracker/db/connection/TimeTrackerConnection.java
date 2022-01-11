package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.nyaacore.orm.NonUniqueResultException;
import cat.nyaa.nyaacore.orm.WhereClause;
import cat.nyaa.nyaacore.orm.backends.IConnectedDatabase;
import cat.nyaa.nyaacore.orm.backends.ITypedTable;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record TimeTrackerConnection(
        ITypedTable<TimeTrackerDbModel> timeTrackerTable, IConnectedDatabase db) {

    @Nullable
    public TimeTrackerDbModel getPlayerTimeTracker(UUID playerId) {
        try {
            return timeTrackerTable.selectUnique(WhereClause.EQ("player", playerId));
        } catch (NonUniqueResultException e) {
            return null;
        }
    }
}
