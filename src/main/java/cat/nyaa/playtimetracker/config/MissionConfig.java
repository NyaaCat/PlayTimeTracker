package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.playtimetracker.config.data.MissionData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
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
    }
}
