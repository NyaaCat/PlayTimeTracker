package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.config.data.MissionData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MissionConfig extends FileConfigure {
    private final PlayTimeTracker plugin;
    @Serializable
    public List<MissionData> missionList = new ArrayList<>();
    {
        missionList.add(new MissionData());
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
