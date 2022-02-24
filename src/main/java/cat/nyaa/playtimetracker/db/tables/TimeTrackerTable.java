package cat.nyaa.playtimetracker.db.tables;

import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;

public class TimeTrackerTable {

    static HikariDataSource ds;

    public TimeTrackerTable(HikariDataSource ds) {
        TimeTrackerTable.ds = ds;
    }

    public void deletePlayer(@NotNull UUID playerId) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("DELETE FROM time WHERE player = ?")) {
                    ps.setObject(1, playerId.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void insertPlayer(@NotNull TimeTrackerDbModel model) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("INSERT INTO time (player, lastSeen, dailyTime, weeklyTime, monthlyTime, totalTime) VALUES (?,?,?,?,?,?)")) {
                    ps.setObject(1, model.playerUniqueId.toString());
                    ps.setObject(2, model.lastSeen);
                    ps.setObject(3, model.dailyTime);
                    ps.setObject(4, model.weeklyTime);
                    ps.setObject(5, model.monthlyTime);
                    ps.setObject(6, model.totalTime);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    public TimeTrackerDbModel selectPlayer(@NotNull UUID playerId) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("SELECT * FROM time WHERE player = ?")) {
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
                e.printStackTrace();
                return null;
            }
        }
    }

    public void update(@NotNull TimeTrackerDbModel model, UUID player) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("UPDATE time SET player = ?, lastSeen = ?, dailyTime = ?, weeklyTime = ?, monthlyTime = ?, totalTime = ? WHERE player = ?")) {
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
                e.printStackTrace();
            }
        }
    }

    public void updateBatch(@NotNull Collection<TimeTrackerDbModel> model) {
        synchronized (DatabaseManager.lock) {
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("UPDATE time SET player = ?, lastSeen = ?, dailyTime = ?, weeklyTime = ?, monthlyTime = ?, totalTime = ? WHERE player = ?")) {
                    for (var m : model) {
                        ps.setObject(1, m.playerUniqueId.toString());
                        ps.setObject(2, m.lastSeen);
                        ps.setObject(3, m.dailyTime);
                        ps.setObject(4, m.weeklyTime);
                        ps.setObject(5, m.monthlyTime);
                        ps.setObject(6, m.totalTime);
                        ps.setObject(7, m.playerUniqueId.toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
