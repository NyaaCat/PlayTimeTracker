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

import javax.annotation.concurrent.NotThreadSafe;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;

@NotThreadSafe
public class TimeTrackerTable {

    private static final Logger logger = Constants.getPluginLogger();
    public static final String TABLE_NAME = "time2";

    private final HikariDataSource ds;

    public TimeTrackerTable(HikariDataSource ds) {
        this.ds = ds;
    }

    public boolean tryCreateTable(Plugin plugin) {
        synchronized (DatabaseManager.lock) {
            try {
                return DatabaseUtils.tryCreateTable(ds, TABLE_NAME, "create_table_time2.sql", plugin,"time", "migrant_table_time_time2.sql");
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
                    ps.setString(1, playerId.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to delete from {}", TABLE_NAME, e);
            }
        }
    }

    public void insert(@NotNull TimeTrackerDbModel model) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " (player, lastUpdate, dailyTime, weeklyTime, monthlyTime, totalTime, lastSeen) VALUES (?,?,?,?,?,?,?)")) {
                    ps.setString(1, model.playerUniqueId.toString());
                    ps.setLong(2, model.lastUpdate);
                    ps.setLong(3, model.dailyTime);
                    ps.setLong(4, model.weeklyTime);
                    ps.setLong(5, model.monthlyTime);
                    ps.setLong(6, model.totalTime);
                    ps.setLong(7, model.lastSeen);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to insert into {}", TABLE_NAME, e);
            }
        }
    }

    public void insertBatch(@NotNull Collection<TimeTrackerDbModel> model) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " (player, lastUpdate, dailyTime, weeklyTime, monthlyTime, totalTime, lastSeen) VALUES (?,?,?,?,?,?,?)")) {
                    for (var m : model) {
                        ps.setString(1, m.playerUniqueId.toString());
                        ps.setLong(2, m.lastUpdate);
                        ps.setLong(3, m.dailyTime);
                        ps.setLong(4, m.weeklyTime);
                        ps.setLong(5, m.monthlyTime);
                        ps.setLong(6, m.totalTime);
                        ps.setLong(7, m.lastSeen);
                        ps.addBatch();
                    }
                    ps.executeBatch();
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
                        if (rs.next()) {
                            var _playerId = UUID.fromString(rs.getString(1));
                            var lastUpdate = rs.getLong(2);
                            var dailyTime = rs.getLong(3);
                            var weeklyTIme = rs.getLong(4);
                            var monthlyTime = rs.getLong(5);
                            var totalTime = rs.getLong(6);
                            var lastSeen = rs.getLong(7);
                            return new TimeTrackerDbModel(_playerId, lastUpdate, dailyTime, weeklyTIme, monthlyTime, totalTime, lastSeen);
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

    public void update(@NotNull TimeTrackerDbModel model) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("UPDATE " + TABLE_NAME + " SET lastUpdate = ?, dailyTime = ?, weeklyTime = ?, monthlyTime = ?, totalTime = ?, lastSeen = ? WHERE player = ?")) {
                    ps.setLong(1, model.lastUpdate);
                    ps.setLong(2, model.dailyTime);
                    ps.setLong(3, model.weeklyTime);
                    ps.setLong(4, model.monthlyTime);
                    ps.setLong(5, model.totalTime);
                    ps.setLong(6, model.lastSeen);
                    ps.setString(7, model.playerUniqueId.toString());
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
                try (var ps = conn.prepareStatement("UPDATE " + TABLE_NAME + " SET lastUpdate = ?, dailyTime = ?, weeklyTime = ?, monthlyTime = ?, totalTime = ?, lastSeen = ? WHERE player = ?")) {
                    for (var m : model) {
                        ps.setLong(1, m.lastUpdate);
                        ps.setLong(2, m.dailyTime);
                        ps.setLong(3, m.weeklyTime);
                        ps.setLong(4, m.monthlyTime);
                        ps.setLong(5, m.totalTime);
                        ps.setLong(6, m.lastSeen);
                        ps.setString(7, m.playerUniqueId.toString());
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
