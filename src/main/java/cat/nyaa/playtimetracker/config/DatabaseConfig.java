package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.nyaacore.orm.backends.BackendConfig;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseConfig extends FileConfigure {
    private final PlayTimeTracker plugin;
    @Serializable(name = "backend-config")
    public BackendConfig backendConfig = BackendConfig.sqliteBackend("pluginDatabase.db");

    public DatabaseConfig(PlayTimeTracker plugin) {
        this.plugin = plugin;
    }

    @Override
    protected String getFileName() {
        return "databaseConfig.yml";
    }

    @Override
    public JavaPlugin getPlugin() {
        return this.plugin;
    }
}
