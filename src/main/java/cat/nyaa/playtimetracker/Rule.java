package cat.nyaa.playtimetracker;

import org.bukkit.configuration.ConfigurationSection;

public class Rule {
    public enum PeriodType {
        DAY,
        WEEK,
        MONTH,
        DISPOSABLE,
        SESSION,
        LONGTIMENOSEE;
    }
    public String name;
    public PeriodType period;
    public long require;
    public boolean autoGive;
    public long timeout;
    public String reward;

    public Rule(String name, ConfigurationSection s) {
        this.name = name;
        switch (s.getString("period")){
            case "day": {period = PeriodType.DAY;break;}
            case "week": {period = PeriodType.WEEK;break;}
            case "month": {period = PeriodType.MONTH;break;}
            case "disposable": {period = PeriodType.DISPOSABLE;break;}
            case "session": {period = PeriodType.SESSION;break;}
            case "longtimenosee": {period = PeriodType.LONGTIMENOSEE;break;}
        }
        require = s.getLong("require");
        autoGive = s.getBoolean("auto-give");
        timeout = s.contains("timeout")?s.getLong("timeout"):-1;
        reward = s.getString("reward");
    }
}
