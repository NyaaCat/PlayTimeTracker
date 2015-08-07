package cat.nyaa.playtimetracker;

import org.bukkit.configuration.ConfigurationSection;

public class Locale {
    private static ConfigurationSection lang;
    private static String prefix;

    public static void init(ConfigurationSection langSection) {
        lang = langSection;
        prefix = lang.getString("prefix");
    }

    public static String get(String name, String... args) {
        return prefix + String.format(lang.getString(name), args);
    }

    public static String formatTime(long ms) {
        String str = "";
        if (ms == 0)
            return "0";

        if (ms > 0) {
            str = String.format("%dms", ms % 1000) + str;
            ms = Math.floorDiv(ms, 1000);
        }
        if (ms > 0) {
            str = String.format("%ds ", ms % 60) + str;
            ms = Math.floorDiv(ms, 60);
        }
        if (ms > 0) {
            str = String.format("%dm ", ms % 60) + str;
            ms = Math.floorDiv(ms, 60);
        }
        if (ms > 0) {
            str = String.format("%dh ", ms % 24) + str;
            ms = Math.floorDiv(ms, 24);
        }
        if (ms > 0) {
            str = String.format("%dd ", ms) + str;
        }

        return str;
    }
}
