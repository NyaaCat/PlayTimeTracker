package cat.nyaa.playtimetracker.db.tables;

import cat.nyaa.nyaacore.orm.BundledSQLUtils;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompletedMissionTable {

    private static final Logger logger = LoggerFactory.getLogger(CompletedMissionTable.class);

    public static final String TABLE_NAME = "completed";
    private final HikariDataSource ds;

    public CompletedMissionTable(HikariDataSource ds) {
        this.ds = ds;
    }

    public boolean tryCreateTable(Plugin plugin) {
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                final String[] types = { "TABLE" };
                ResultSet rs = conn.getMetaData().getTables(null, null, TABLE_NAME, types);
                if(rs.next()){
                    return false;
                }
                BundledSQLUtils.queryBundledAs(plugin, conn, "create_table_completed.sql", null, null);
                return true;
            } catch (SQLException e) {
                logger.error("Failed to create {}", TABLE_NAME, e);
                return false;
            }
        }
    }

    public void delete(@NotNull UUID playerId, String missionName) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE (player = ? AND mission = ?)")) {
                    ps.setObject(1, playerId.toString());
                    ps.setObject(2, missionName);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to delete from {}", TABLE_NAME, e);
            }
        }
    }

    public void delete(@NotNull UUID playerId) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE player = ?")) {
                    ps.setObject(1, playerId.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to delete from {}", TABLE_NAME, e);
            }
        }
    }

    public void insert(@NotNull CompletedMissionDbModel model) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " (lastCompleted, mission, player) VALUES (?,?,?)")) {
                    ps.setObject(1, model.lastCompletedTime);
                    ps.setObject(2, model.missionName);
                    ps.setObject(3, model.playerUniqueId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to insert into {}", TABLE_NAME, e);
            }
        }
    }

    @NotNull
    public List<CompletedMissionDbModel> select(@NotNull UUID playerId, @Nullable String missionName) {
        synchronized (DatabaseManager.lock) {
            var sql = "SELECT * FROM " + TABLE_NAME + " WHERE player = ?";
            var res = new ArrayList<CompletedMissionDbModel>();
            if (missionName != null) {
                sql = "SELECT * FROM " + TABLE_NAME + " WHERE (player = ? AND mission = ?)";
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
                logger.error("Failed to select from {}", TABLE_NAME, e);
                return new ArrayList<>();
            }
        }
    }

    public void updatePlayer(CompletedMissionDbModel model, int id) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("UPDATE " + TABLE_NAME + " SET lastCompleted = ?, mission = ?, player = ? WHERE id = ?")) {
                    ps.setObject(1, model.lastCompletedTime);
                    ps.setObject(2, model.missionName);
                    ps.setObject(3, model.playerUniqueId);
                    ps.setObject(4, id);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to update {}", TABLE_NAME, e);
            }
        }
    }
}
