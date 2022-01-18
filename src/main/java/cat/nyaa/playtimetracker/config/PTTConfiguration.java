package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;

public class PTTConfiguration extends PluginConfigure {

    @StandaloneConfig
    public final DatabaseConfig databaseConfig;
    @StandaloneConfig
    public final MissionConfig missionConfig;

    private final PlayTimeTracker plugin;

    @Serializable
    public String language = "en_US";
    @Serializable
    public String timezone = ZoneId.systemDefault().getId();
    @Serializable(name = "check-afk")
    public boolean checkAfk = true;
    @Serializable(name = "use-ess-afk-status")
    public boolean useEssAfkStatus = true;
    @Serializable(name = "afk-time-ms")
    public long afkTimeMS = 180 * 1000L;
    @Serializable(name = "check-acquire-tick")
    public long checkAcquireTick = 10 * 20L;

    public PTTConfiguration(PlayTimeTracker plugin) {
        this.plugin = plugin;
        this.databaseConfig = new DatabaseConfig(plugin);
        this.missionConfig = new MissionConfig(plugin);
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }
}
