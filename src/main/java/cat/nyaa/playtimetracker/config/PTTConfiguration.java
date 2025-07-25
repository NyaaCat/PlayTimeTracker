package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;
import java.util.List;

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
        databaseConfig.validate(context);
        missionConfig.validate(context);
    }
}
