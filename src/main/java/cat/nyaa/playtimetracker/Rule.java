package cat.nyaa.playtimetracker;

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
    public Rule(String name, ConfigurationSection s) {
        this.name = name;
        switch (s.getString("period")) {
            case "day" -> period = PeriodType.DAY;
            case "week" -> period = PeriodType.WEEK;
            case "month" -> period = PeriodType.MONTH;
            case "disposable" -> period = PeriodType.DISPOSABLE;
            case "session" -> period = PeriodType.SESSION;
            case "longtimenosee" -> period = PeriodType.LONGTIMENOSEE;
        }
        require = s.getLong("require");
        autoGive = s.getBoolean("auto-give");
        timeout = s.contains("timeout") ? s.getLong("timeout") : -1;
        group = s.contains("eligible-group") ? new HashSet<>(s.getStringList("eligible-group")) : null;
        reward = s.getString("reward");
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
