package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.nyaacore.orm.WhereClause;
import cat.nyaa.nyaacore.orm.backends.IConnectedDatabase;
import cat.nyaa.nyaacore.orm.backends.ITypedTable;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record CompletedMissionConnection(ITypedTable<CompletedMissionDbModel> completedMissionTable,
                                         IConnectedDatabase db) {

    public void resetPlayerCompletedMission(UUID playerId) {
        completedMissionTable.delete(WhereClause.EQ("player", playerId));
    }

    public void resetPlayerCompletedMission(String missionName, UUID playerUniqueId) {
        completedMissionTable.delete(WhereClause.EQ("player", playerUniqueId)
                .whereEq("mission", missionName));
    }

    public void WriteMissionCompleted(UUID playerUniqueId, String missionName, long lastCompletedTime) {
        synchronized (CompletedMissionConnection.class) {
            CompletedMissionDbModel model = completedMissionTable.selectUniqueUnchecked(
                    WhereClause.EQ("player", playerUniqueId)
                            .whereEq("mission", missionName)
            );
            if (model == null) {
                CompletedMissionDbModel newModel = new CompletedMissionDbModel();
                newModel.setMissionName(missionName);
                newModel.setLastCompletedTime(lastCompletedTime);
                newModel.setPlayerUniqueId(playerUniqueId);
                completedMissionTable.insert(newModel);
            } else {
                model.setLastCompletedTime(lastCompletedTime);
                completedMissionTable.update(model, WhereClause.EQ("id", model.getId()), "lastCompleted");
            }
        }
    }

    @Nullable
    public CompletedMissionDbModel getPlayerCompletedMission(UUID playerUniqueId, String missionName) {
        return completedMissionTable.selectUniqueUnchecked(WhereClause.EQ("player", playerUniqueId)
                .whereEq("mission", missionName));
    }

    @Nullable
    public List<CompletedMissionDbModel> getPlayerCompletedMissionList(UUID playerUniqueId) {
        return completedMissionTable.select(WhereClause.EQ("player", playerUniqueId));
    }
}
