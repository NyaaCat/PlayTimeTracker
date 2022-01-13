package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;

public class PTTConfiguration extends PluginConfigure {
    @StandaloneConfig
    public final DatabaseConfig databaseConfig;
    private final PlayTimeTracker plugin;
    @Serializable
    public String language = "en_US";
    @Serializable
    public String timezone = ZoneId.systemDefault().getId();
    @Serializable(name = "check-afk")
    public boolean checkAfk;
    @Serializable(name = "use-ess-afk-status")
    public boolean useEssAfkStatus;
    @Serializable(name = "afk-time-ms")
    public long afkTimeMS;

    public PTTConfiguration(PlayTimeTracker plugin) {
        this.plugin = plugin;
        this.databaseConfig = new DatabaseConfig(plugin);
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }
}
