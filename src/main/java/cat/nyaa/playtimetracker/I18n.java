package cat.nyaa.playtimetracker;

import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class I18n extends LanguageRepository {
    @Nullable
    private static I18n instance = null;
    private final PlayTimeTracker plugin;
    private final String lang;

    public I18n(PlayTimeTracker plugin, String lang) {
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
    protected String getLanguage() {
        return lang;
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
}