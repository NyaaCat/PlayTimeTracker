package cat.nyaa.playtimetracker;

import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class I18n extends LanguageRepository {
    @Nullable
    private static I18n instance;
    private final PTT plugin;
    private final String lang;

    public I18n(PTT plugin, String lang) {
        instance = this;
        this.plugin = plugin;
        this.lang = lang;
        load();
    }

    @Override
    protected Plugin getPlugin() {
        return plugin;
    }

    @Override
    @SuppressWarnings({"deprecation"})
    protected String getLanguage() {
        return lang;
    }

    public static String formatWithColor(String key, Object... args) {
        return ChatColor.translateAlternateColorCodes('&', I18n.format(key, args));
    }

    public static String format(String key, Object... args) {
        if (instance == null) return "<Not initialized>";
        return instance.getFormatted(key, args);
    }

    public static String substitute(String key, Object... args) {
        if (instance == null) return "<Not initialized>";
        return instance.getSubstituted(key, args);
    }

    public static void send(CommandSender recipient, String key, Object... args) {
        recipient.sendMessage(format(key, args));
    }

    public static String formatTime(long ms) {
        String str = "";
        if (ms == 0)
            return formatWithColor("info.statistic.time.format.zero");
        if (ms > 0) {
            str = formatWithColor("info.statistic.time.format.ms", ms % 1000) + str;
            ms = Math.floorDiv(ms, 1000);
        }
        if (ms > 0) {
            str = formatWithColor("info.statistic.time.format.s", ms % 60) + str;
            ms = Math.floorDiv(ms, 60);
        }
        if (ms > 0) {
            str = formatWithColor("info.statistic.time.format.m", ms % 60) + str;
            ms = Math.floorDiv(ms, 60);
        }
        if (ms > 0) {
            str = formatWithColor("info.statistic.time.format.h", ms ) + str;
        }

        return str;
    }

//    private static String getConfigStringWithColor(ConfigurationSection section, String key) {
//        return ChatColor.translateAlternateColorCodes('&', section.getString(key));
//    }
//
//    public static String formatTime(long ms) {
//        ConfigurationSection s = lang.getConfigurationSection("statistic-time-format");
//        String str = "";
//        if (ms == 0)
//            return getConfigStringWithColor(s, "zero");
//
//        if (ms > 0) {
//            str = String.format(getConfigStringWithColor(s, "ms"), ms % 1000) + str;
//            ms = Math.floorDiv(ms, 1000);
//        }
//        if (ms > 0) {
//            str = String.format(getConfigStringWithColor(s, "s"), ms % 60) + str;
//            ms = Math.floorDiv(ms, 60);
//        }
//        if (ms > 0) {
//            str = String.format(getConfigStringWithColor(s, "m"), ms % 60) + str;
//            ms = Math.floorDiv(ms, 60);
//        }
//        if (ms > 0) {
//            str = String.format(getConfigStringWithColor(s, "h"), ms) + str;
//        }
//
//        return str;
//    }

}
