package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.nyaacore.orm.WhereClause;
import cat.nyaa.nyaacore.orm.backends.IConnectedDatabase;
import cat.nyaa.nyaacore.orm.backends.ITypedTable;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record CompletedMissionConnection(ITypedTable<CompletedMissionDbModel> completedMissionTable,
                                         IConnectedDatabase db) {

    public void deletePlayerData(UUID playerId) {
        completedMissionTable.delete(WhereClause.EQ("player", playerId));
    }

    public void resetAllMissionData(String missionName, UUID playerUniqueId) {
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

    public List<CompletedMissionDbModel> getPlayerCompletedMissionBefore(UUID playerUniqueId, long timeStamp, String... missionNames) {
        if (missionNames == null || missionNames.length == 0) {
            return completedMissionTable.select(
                    WhereClause.EQ("player", playerUniqueId)
                            .where("lastCompleted", "<", timeStamp)
            );
        }
        List<String> missionNameList = Arrays.stream(missionNames).map(s -> "mission=" + s).toList();
        String sql = "SELECT * FROM " + completedMissionTable.getTableName() + " WHERE player=" + playerUniqueId + " AND lastCompleted<" + timeStamp + " AND (" + String.join(" OR ", missionNameList) + ")";
        List<CompletedMissionDbModel> result = new ArrayList<>();
        try (ResultSet rs = db.getConnection().createStatement().executeQuery(sql)) {
            while (rs.next()) {
                CompletedMissionDbModel obj = completedMissionTable.getJavaTypeModifier().getObjectFromResultSet(rs);
                result.add(obj);
            }
        } catch (SQLException | ReflectiveOperationException e) {
            e.printStackTrace();
            return result;
        }
        return result;
    }

    @Nullable
    public CompletedMissionDbModel getPlayerCompletedMission(UUID playerUniqueId, String missionName) {
        return completedMissionTable.selectUniqueUnchecked(WhereClause.EQ("player", playerUniqueId)
                .whereEq("mission", missionName));
    }
}
