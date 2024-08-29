package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.config.data.ISerializableExt;
import cat.nyaa.playtimetracker.config.data.MissionData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MissionConfig extends FileConfigure {
    private final JavaPlugin plugin;
    @Serializable
    public Map<String, MissionData> missions = new LinkedHashMap<>();

    {
        missions.put("test", new MissionData());
    }

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
    public void load() {
        super.load();
        for (MissionData missionData : missions.values()) {
            if(!missionData.validate()) {
                throw new RuntimeException("Invalid mission data");
            }
        }
    }
}
