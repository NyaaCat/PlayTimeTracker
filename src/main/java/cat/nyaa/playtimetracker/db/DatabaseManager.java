package cat.nyaa.playtimetracker.db;

import cat.nyaa.playtimetracker.config.DatabaseConfig;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.connection.RewardsConnection;
import cat.nyaa.playtimetracker.db.connection.TimeTrackerConnection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    public static final UUID lock = UUID.randomUUID();
    private final DatabaseConfig databaseConfig;
    HikariDataSource ds;
    private TimeTrackerConnection timeTrackerConnection;
    private CompletedMissionConnection completedMissionConnection;
    private RewardsConnection rewardsConnection;

    public DatabaseManager(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        File f = new File(databaseConfig.getPlugin().getDataFolder(), databaseConfig.backendConfig.sqlite_file);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + f.getAbsolutePath());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
        loadTables();
    }

    private void loadTables() {
        this.timeTrackerConnection = new TimeTrackerConnection(ds, databaseConfig.getPlugin());
        this.completedMissionConnection = new CompletedMissionConnection(ds, databaseConfig.getPlugin());
        this.rewardsConnection = new RewardsConnection(ds, databaseConfig.getPlugin());
    }

    public void close() {
        this.timeTrackerConnection.close();
        this.completedMissionConnection.close();
        this.rewardsConnection.close();
        ds.close();
    }

    public void setDbSynchronous(DbSynchronousType type) {
        try {
            ds.getConnection().createStatement().execute("PRAGMA synchronous = " + type.name() + ";");
        } catch (SQLException e) {
            logger.error("Failed to set synchronous", e);
        }
    }

    public TimeTrackerConnection getTimeTrackerConnection() {
        return this.timeTrackerConnection;
    }

    public CompletedMissionConnection getCompletedMissionConnection() {
        return this.completedMissionConnection;
    }

    public RewardsConnection getRewardsConnection() {
        return this.rewardsConnection;
    }
}
