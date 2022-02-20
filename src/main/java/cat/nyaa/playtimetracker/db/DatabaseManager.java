package cat.nyaa.playtimetracker.db;

import cat.nyaa.nyaacore.orm.BundledSQLUtils;
import cat.nyaa.playtimetracker.config.DatabaseConfig;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.connection.TimeTrackerConnection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseManager {
    private final DatabaseConfig databaseConfig;
    HikariDataSource ds;
    private TimeTrackerConnection timeTrackerConnection;
    private CompletedMissionConnection completedMissionConnection;

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
        createTables();
        this.timeTrackerConnection = new TimeTrackerConnection(ds, databaseConfig.getPlugin());
        this.completedMissionConnection = new CompletedMissionConnection(ds);
    }

    private boolean tableExists(String tableName, Connection dbConn) throws SQLException {
        try (ResultSet rs = dbConn.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    public void createTables() {
        try (var conn = ds.getConnection()) {
            if (!tableExists("time", conn)) {
                BundledSQLUtils.queryBundledAs(databaseConfig.getPlugin(), conn, "create_table_time.sql", null, null);
            }
            if (!tableExists("completed", conn)) {
                BundledSQLUtils.queryBundledAs(databaseConfig.getPlugin(), conn, "create_table_completed.sql", null, null);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    public void close() {
        this.timeTrackerConnection.close();
        this.completedMissionConnection.close();
        ds.close();
    }

    public void setDbSynchronous(DbSynchronousType type) {
        try {
            ds.getConnection().createStatement().execute("PRAGMA synchronous = " + type.name() + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public TimeTrackerConnection getTimeTrackerConnection() {
        return this.timeTrackerConnection;
    }

    public CompletedMissionConnection getCompletedMissionConnection() {
        return this.completedMissionConnection;
    }

}
