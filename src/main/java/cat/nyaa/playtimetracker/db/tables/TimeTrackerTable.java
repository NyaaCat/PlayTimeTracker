package cat.nyaa.playtimetracker.db.tables;

import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.db.utils.DatabaseUtils;
import cat.nyaa.playtimetracker.utils.Constants;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;

public class TimeTrackerTable {

    private static final Logger logger = Constants.getPluginLogger();
    public static final String TABLE_NAME = "time";

    private final HikariDataSource ds;

    public TimeTrackerTable(HikariDataSource ds) {
        this.ds = ds;
    }

    public boolean tryCreateTable(Plugin plugin) {
        synchronized (DatabaseManager.lock) {
            try {
                return DatabaseUtils.tryCreateTable(ds, TABLE_NAME, "create_table_time.sql", plugin);
            } catch (SQLException e) {
                logger.error("Failed to create {}", TABLE_NAME, e);
                return false;
            }
        }
    }

    public void deletePlayer(@NotNull UUID playerId) {
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

    public void insertPlayer(@NotNull TimeTrackerDbModel model) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " (player, lastSeen, dailyTime, weeklyTime, monthlyTime, totalTime) VALUES (?,?,?,?,?,?)")) {
                    ps.setObject(1, model.playerUniqueId.toString());
                    ps.setObject(2, model.lastSeen);
                    ps.setObject(3, model.dailyTime);
                    ps.setObject(4, model.weeklyTime);
                    ps.setObject(5, model.monthlyTime);
                    ps.setObject(6, model.totalTime);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to insert into {}", TABLE_NAME, e);
            }
        }
    }

    @Nullable
    public TimeTrackerDbModel selectPlayer(@NotNull UUID playerId) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE player = ?")) {
                    ps.setObject(1, playerId.toString());
                    try (var rs = ps.executeQuery()) {
                        while (rs.next()) {
                            var dailyTime = rs.getLong(1);
                            var lastSeen = rs.getLong(2);
                            var monthlyTime = rs.getLong(3);
                            var _playerId = UUID.fromString(rs.getString(4));
                            var totalTime = rs.getLong(5);
                            var weeklyTIme = rs.getLong(6);
                            return new TimeTrackerDbModel(_playerId, lastSeen, dailyTime, weeklyTIme, monthlyTime, totalTime);
                        }
                        return null;
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to select from {}", TABLE_NAME, e);
                return null;
            }
        }
    }

    public void update(@NotNull TimeTrackerDbModel model, UUID player) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("UPDATE " + TABLE_NAME + " SET player = ?, lastSeen = ?, dailyTime = ?, weeklyTime = ?, monthlyTime = ?, totalTime = ? WHERE player = ?")) {
                    ps.setObject(1, model.playerUniqueId.toString());
                    ps.setObject(2, model.lastSeen);
                    ps.setObject(3, model.dailyTime);
                    ps.setObject(4, model.weeklyTime);
                    ps.setObject(5, model.monthlyTime);
                    ps.setObject(6, model.totalTime);
                    ps.setObject(7, player);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to update {}", TABLE_NAME, e);
            }
        }
    }

    public void updateBatch(@NotNull Collection<TimeTrackerDbModel> model) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("UPDATE " + TABLE_NAME + " SET lastSeen = ?, dailyTime = ?, weeklyTime = ?, monthlyTime = ?, totalTime = ? WHERE player = ?")) {
                    for (var m : model) {
                        ps.setObject(1, m.lastSeen);
                        ps.setObject(2, m.dailyTime);
                        ps.setObject(3, m.weeklyTime);
                        ps.setObject(4, m.monthlyTime);
                        ps.setObject(5, m.totalTime);
                        ps.setObject(6, m.playerUniqueId.toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                logger.error("Failed to update {}", TABLE_NAME, e);
            }
        }
    }
}
