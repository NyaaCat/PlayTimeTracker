package cat.nyaa.playtimetracker.config;

import cat.nyaa.nyaacore.configuration.PluginConfigure;
import cat.nyaa.playtimetracker.PTT;
import cat.nyaa.playtimetracker.config.data.RewardData;
import cat.nyaa.playtimetracker.config.data.RuleData;
import com.google.common.collect.Lists;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class PTTConfiguration extends PluginConfigure {
    private final PTT plugin;

    public PTTConfiguration(PTT plugin) {
        this.plugin = plugin;
    }

    @Override
    protected JavaPlugin getPlugin() {
        return this.plugin;
    }


    @Serializable
    public String language = "en_US";

    @Serializable(name = "check-afk")
    public boolean checkAfk = true;

    @Serializable(name = "use-ess-afk-status")
    public boolean useEssAfkStatus = false;

    @Serializable(name ="afk-time")
    public int afkTime = 180;//seconds

    @Serializable(name ="afk-check-interval")
    public Long afkCheckInterval = 30L;//seconds

    @Serializable(name = "cancel-afk-on.chat")
    public boolean cancelAfkOnChat = true;
    @Serializable(name = "cancel-afk-on.command")
    public boolean cancelAfkOnCommand = true;
    @Serializable(name = "cancel-afk-on.move")
    public boolean cancelAfkOnMove = true;

    @Serializable(name = "save-interval")
    public long saveInterval = 60L;//seconds

    @Serializable(name = "display-on-login")
    public boolean DisplayOnLogin = false;
    @Serializable(name = "rewards")
    public Map<String, RewardData> rewards = new LinkedHashMap<>();
    {
        rewards.put("some-reward",new RewardData("You were given one stone.","give {playerName} 1 1"));
        rewards.put("another-reward",new RewardData("You were give two stones","give {playerName} 1 2"));
        rewards.put("third-reward",new RewardData("","broadcast {playerName} has been online for one hour!"));
    }

    @Serializable(name = "rules")
    public Map<String, RuleData> rules = new LinkedHashMap<>();
    {
        rules.put("rule1",new RuleData("day",60,false,60,"some-reward",null));
        rules.put("rule2",new RuleData("disposable",60,true,-1,"third-reward",null));
        rules.put("rule3",new RuleData("longtimenosee",60,true,-1,"another-reward", Lists.newArrayList("group1","group2")));
    }

}
