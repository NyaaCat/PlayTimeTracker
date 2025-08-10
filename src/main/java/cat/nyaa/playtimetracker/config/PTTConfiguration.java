package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;

public class PTTConfiguration extends PluginConfigure implements ISerializableExt {

    @StandaloneConfig
    public final DatabaseConfig databaseConfig;
    @StandaloneConfig
    public final MissionConfig missionConfig;

    private final JavaPlugin plugin;

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
    @Serializable(name = "timer-interval-ms")
    public long timerIntervalMS = 0L; // default is checkAcquireTick in milliseconds
    @Serializable(name = "sync-interval-tick")
    public long syncIntervalTick = 10L; // default is 10 tick

    public PTTConfiguration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseConfig = new DatabaseConfig(plugin);
        this.missionConfig = new MissionConfig(plugin);
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public void validate(IValidationContext context) throws Exception {
        // TODO: validate this
        if (this.checkAfk && !this.useEssAfkStatus) {
            if (this.afkTimeMS <= 0) {
                throw new IllegalArgumentException("afk-time-ms");
            }
        }
        if (this.timerIntervalMS == 0) {
            if (this.checkAcquireTick > 0) {
                this.timerIntervalMS = this.checkAcquireTick * 50L; // convert to milliseconds
            }
        }
        if (this.timerIntervalMS <= 0) {
            throw new IllegalArgumentException("timer-interval-ms");
        }
        if (this.syncIntervalTick <= 0) {
            throw new IllegalArgumentException("sync-interval-tick");
        }
        databaseConfig.validate(context);
        missionConfig.validate(context);
    }
}
