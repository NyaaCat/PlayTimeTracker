package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.config.data.ISerializableExt;
import cat.nyaa.playtimetracker.config.data.MissionData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
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
        List<String> outputError = new ObjectArrayList<>(4);
        for (var mission : missions.entrySet()) {
            if(!mission.getValue().validate(outputError)) {
                outputError.add("Invalid mission data: " + mission.getKey());
                StringBuilder sb = new StringBuilder("Parse error in MissionConfig: ");
                for (int i = outputError.size() - 1; i >= 0; i--) {
                    sb.append("\r\n\t").append(outputError.get(i));
                }
                throw new RuntimeException(sb.toString());
            }
        }
    }
}
