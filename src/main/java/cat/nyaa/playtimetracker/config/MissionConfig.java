package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.playtimetracker.config.data.MissionData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class MissionConfig extends FileConfigure implements ISerializableExt {
    private final JavaPlugin plugin;
    @Serializable
    public Map<String, MissionData> missions = new LinkedHashMap<>();

    {
        missions.put("test", new MissionData());
    }

    @Serializable(name = "login-check-delay-ticks")
    public int loginCheckDelayTicks = 20;
    
    @Serializable(name = "sync-ref-cache-time")
    public long syncRefCacheTime = 256 * 50; // sync ref vault by cache its value, keeps $value milliseconds; default set to 4 * 64 gt

    public MissionConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected String getFileName() {
        return "mission.yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public void validate(IValidationContext context) throws Exception {
        for (var mission : missions.entrySet()) {
            mission.getValue().validate(context);
        }
        if(loginCheckDelayTicks < 0) {
            throw new IllegalArgumentException("loginCheckDelayTicks must be non-negative");
        }
        if(syncRefCacheTime < 0) {
            throw new IllegalArgumentException("syncRefCacheTime must be non-negative");
        }
    }
}
