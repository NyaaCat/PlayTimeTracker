package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.nyaacore.orm.backends.BackendConfig;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseConfig extends FileConfigure {
    private final PlayTimeTracker plugin;

    public DatabaseConfig(PlayTimeTracker plugin) {
        this.plugin = plugin;
    }

    @Serializable
    public BackendConfig backendConfig = BackendConfig.sqliteBackend("dataBase.db");

    @Override
    protected String getFileName() {
        return "database.yml";
    }

    @Override
    public JavaPlugin getPlugin() {
        return this.plugin;
    }
}