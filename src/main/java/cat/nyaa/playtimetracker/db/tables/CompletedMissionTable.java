package cat.nyaa.playtimetracker.db.tables;

import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompletedMissionTable {
    private final HikariDataSource ds;

    public CompletedMissionTable(HikariDataSource ds) {
        this.ds = ds;
    }

    public void delete(@NotNull UUID playerId, String missionName) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("DELETE FROM completed WHERE (player = ? AND mission = ?)")) {
                    ps.setObject(1, playerId.toString());
                    ps.setObject(2, missionName);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void delete(@NotNull UUID playerId) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("DELETE FROM completed WHERE player = ?")) {
                    ps.setObject(1, playerId.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void insert(@NotNull CompletedMissionDbModel model) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("INSERT INTO completed (lastCompleted, mission, player) VALUES (?,?,?)")) {
                    ps.setObject(1, model.lastCompletedTime);
                    ps.setObject(2, model.missionName);
                    ps.setObject(3, model.playerUniqueId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @NotNull
    public List<CompletedMissionDbModel> select(@NotNull UUID playerId, @Nullable String missionName) {
        synchronized (DatabaseManager.lock) {
            var sql = "SELECT * FROM completed WHERE player = ?";
            var res = new ArrayList<CompletedMissionDbModel>();
            if (missionName != null) {
                sql = "SELECT * FROM completed WHERE (player = ? AND mission = ?)";
            }
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setObject(1, playerId.toString());
                    if (missionName != null) {
                        ps.setObject(2, missionName);
                    }
                    try (var rs = ps.executeQuery()) {
                        while (rs.next()) {
                            var id = rs.getInt(1);
                            var lastCompleted = rs.getLong(2);
                            var mission = rs.getString(3);
                            var player = UUID.fromString(rs.getString(4));
                            res.add(new CompletedMissionDbModel(id, player, mission, lastCompleted));
                        }
                        return res;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
    }

    public void updatePlayer(CompletedMissionDbModel model, int id) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("UPDATE completed SET id = ?, lastCompleted = ?, mission = ?, player = ? WHERE id = ?")) {
                    ps.setObject(1, model.id);
                    ps.setObject(2, model.lastCompletedTime);
                    ps.setObject(3, model.missionName);
                    ps.setObject(4, model.playerUniqueId);
                    ps.setObject(5, id);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
