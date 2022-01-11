package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import org.bukkit.plugin.java.JavaPlugin;

public class PTTConfiguration extends PluginConfigure {
    private final PlayTimeTracker plugin;

    public PTTConfiguration(PlayTimeTracker plugin) {
        this.plugin = plugin;
        this.databaseConfig = new DatabaseConfig(plugin);
    }

    @StandaloneConfig
    public final DatabaseConfig databaseConfig;
    @Serializable
    public String language = "en_US";

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }
}
