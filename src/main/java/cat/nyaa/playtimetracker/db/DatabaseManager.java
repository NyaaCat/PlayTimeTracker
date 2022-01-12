package cat.nyaa.playtimetracker.db;

import cat.nyaa.nyaacore.orm.DatabaseUtils;
import cat.nyaa.nyaacore.orm.backends.IConnectedDatabase;
import cat.nyaa.nyaacore.orm.backends.ITypedTable;
import cat.nyaa.playtimetracker.config.DatabaseConfig;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.connection.TimeTrackerConnection;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import org.bukkit.Bukkit;

import java.sql.SQLException;

public class DatabaseManager {
    private final DatabaseConfig databaseConfig;
    IConnectedDatabase db;
    private ITypedTable<CompletedMissionDbModel> completedMissionTable;
    private ITypedTable<TimeTrackerDbModel> timeTrackerTable;

    public DatabaseManager(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        try {
            db = DatabaseUtils.connect(databaseConfig.getPlugin(), databaseConfig.backendConfig);
            loadTables();
        } catch (ClassNotFoundException | SQLException e) {
            try {
                db.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(databaseConfig.getPlugin());
        }
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    private void loadTables() {
        this.timeTrackerTable = db.getTable(TimeTrackerDbModel.class);
        this.completedMissionTable = db.getTable(CompletedMissionDbModel.class);

    }

    public void close() {
        try {
            db.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public TimeTrackerConnection getTimeTrackerConnection() {
        return new TimeTrackerConnection(this.timeTrackerTable, db);
    }

    public CompletedMissionConnection getCompletedMissionConnection() {
        return new CompletedMissionConnection(this.completedMissionTable, db);
    }

}
