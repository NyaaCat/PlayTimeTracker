package cat.nyaa.playtimetracker.db;

import cat.nyaa.nyaacore.orm.DatabaseUtils;
import cat.nyaa.nyaacore.orm.backends.IConnectedDatabase;
import cat.nyaa.nyaacore.orm.backends.ITypedTable;
import cat.nyaa.playtimetracker.config.DatabaseConfig;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.connection.TimeTrackerConnection;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private final DatabaseConfig databaseConfig;
    HikariDataSource ds;
    private TimeTrackerConnection timeTrackerConnection;

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

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    private void loadTables() {
        // this.setDbSynchronous(DbSynchronousType.OFF);
//        this.timeTrackerTable = db.getTable(TimeTrackerDbModel.class);
        this.timeTrackerConnection = new TimeTrackerConnection(ds, databaseConfig.getPlugin(), databaseConfig.backendConfig);
//        this.completedMissionTable = db.getTable(CompletedMissionDbModel.class);

    }

    public void close() {
        timeTrackerConnection.close();
        //this.setDbSynchronous(DbSynchronousType.FULL);
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
        return timeTrackerConnection;
    }

    public CompletedMissionConnection getCompletedMissionConnection() {
        return new CompletedMissionConnection(ds);
    }

}
