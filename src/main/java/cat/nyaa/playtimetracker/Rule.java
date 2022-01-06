package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.data.RuleData;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class Rule {
    public String name;
    public PeriodType period;
    public long require;
    public boolean autoGive;
    public long timeout;
    public String reward;
    public Set<String> group;

    public Rule(String name,RuleData ruleData) {
        this.name = name;
        switch (ruleData.period) {
            case "day" -> period = PeriodType.DAY;
            case "week" -> period = PeriodType.WEEK;
            case "month" -> period = PeriodType.MONTH;
            case "disposable" -> period = PeriodType.DISPOSABLE;
            case "session" -> period = PeriodType.SESSION;
            case "longtimenosee" -> period = PeriodType.LONGTIMENOSEE;
        }
        require = ruleData.require;
        autoGive = ruleData.autoGive;
        timeout = ruleData.timeout;
        group = new HashSet<>(ruleData.group);
        reward = ruleData.reward;
    }

    public enum PeriodType {
        DAY,
        WEEK,
        MONTH,
        DISPOSABLE,
        SESSION,
        LONGTIMENOSEE
    }
}
