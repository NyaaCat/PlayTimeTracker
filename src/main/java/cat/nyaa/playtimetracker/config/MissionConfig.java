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
    public boolean validate(List<String> outputError) {
        for (var mission : missions.entrySet()) {
            if(!mission.getValue().validate(outputError)) {
                outputError.add("Invalid mission data: " + mission.getKey());
                return false;
            }
        }
        if(loginCheckDelayTicks < 0) {
            outputError.add("Invalid login-check-delay-ticks (should be positive): " + loginCheckDelayTicks);
            return false;
        }
        return true;
    }

    @Override
    public void load() {
        super.load();
        List<String> outputError = new ObjectArrayList<>(4);
        if(!validate(outputError)) {
            StringBuilder sb = new StringBuilder("Parse error in MissionConfig: ");
            for (int i = outputError.size() - 1; i >= 0; i--) {
                sb.append("\r\n\t").append(outputError.get(i));
            }
            throw new RuntimeException(sb.toString());
        }
    }
}
