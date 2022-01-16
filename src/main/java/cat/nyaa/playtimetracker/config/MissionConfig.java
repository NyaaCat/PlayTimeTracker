package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.config.data.MissionData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MissionConfig extends FileConfigure {
    private final PlayTimeTracker plugin;
    @Serializable
    public Map<String,MissionData> missions = new LinkedHashMap<>();
    {
        missions.put("test",new MissionData());
    }

    public MissionConfig(PlayTimeTracker plugin) {
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
}
