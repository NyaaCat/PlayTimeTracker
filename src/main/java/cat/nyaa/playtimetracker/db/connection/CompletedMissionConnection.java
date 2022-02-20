package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import cat.nyaa.playtimetracker.db.tables.CompletedMissionTable;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class CompletedMissionConnection {
    private final HikariDataSource ds;
    private final CompletedMissionTable completedMissionTable;

    public CompletedMissionConnection(HikariDataSource ds) {
        this.ds = ds;
        this.completedMissionTable = new CompletedMissionTable(ds);

    }

    public void resetPlayerCompletedMission(UUID playerId) {
        completedMissionTable.delete(playerId);
    }

    public void resetPlayerCompletedMission(String missionName, UUID playerUniqueId) {
        completedMissionTable.delete(playerUniqueId, missionName);
    }

    public void WriteMissionCompleted(UUID playerUniqueId, String missionName, long lastCompletedTime) {
        synchronized (CompletedMissionConnection.class) {
            var rs = completedMissionTable.select(playerUniqueId, missionName);
            if (rs.size() == 0) {
                CompletedMissionDbModel newModel = new CompletedMissionDbModel();
                newModel.setMissionName(missionName);
                newModel.setLastCompletedTime(lastCompletedTime);
                newModel.setPlayerUniqueId(playerUniqueId);
                completedMissionTable.insert(newModel);
            } else {
                var model = rs.get(0);
                model.setLastCompletedTime(lastCompletedTime);
                completedMissionTable.updatePlayer(model, model.getId());
            }
        }
    }

    @Nullable
    public CompletedMissionDbModel getPlayerCompletedMission(UUID playerUniqueId, String missionName) {
        var rs = completedMissionTable.select(playerUniqueId, missionName);
        if (rs.size() > 0) {
            return rs.get(0);
        }
        return null;
    }

    @NotNull
    public List<CompletedMissionDbModel> getPlayerCompletedMissionList(UUID playerUniqueId) {
        return completedMissionTable.select(playerUniqueId, null);
    }
    public void close() {}
}
