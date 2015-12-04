package cat.nyaa.playtimetracker;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public class Locale {
    private static ConfigurationSection lang;
    private static String prefix;

    private static String getConfigStringWithColor(ConfigurationSection section, String key) {
        return ChatColor.translateAlternateColorCodes('&', section.getString(key));
    }

    public static void init(ConfigurationSection langSection) {
        lang = langSection;
        prefix = getConfigStringWithColor(lang, "prefix");
    }

    public static String get(String name, String... args) {
        return prefix + String.format(getConfigStringWithColor(lang, name), args);
    }

    public static String formatTime(long ms) {
        ConfigurationSection s = lang.getConfigurationSection("statistic-time-format");
        String str = "";
        if (ms == 0)
            return getConfigStringWithColor(s,"zero");

        if (ms > 0) {
            str = String.format(getConfigStringWithColor(s,"ms"), ms % 1000) + str;
            ms = Math.floorDiv(ms, 1000);
        }
        if (ms > 0) {
            str = String.format(getConfigStringWithColor(s,"s"), ms % 60) + str;
            ms = Math.floorDiv(ms, 60);
        }
        if (ms > 0) {
            str = String.format(getConfigStringWithColor(s,"m"), ms % 60) + str;
            ms = Math.floorDiv(ms, 60);
        }
        if (ms > 0) {
            str = String.format(getConfigStringWithColor(s,"h"), ms) + str;
        }

        return str;
    }
}
