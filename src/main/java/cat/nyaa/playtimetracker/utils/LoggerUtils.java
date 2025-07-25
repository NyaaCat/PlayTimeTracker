package cat.nyaa.playtimetracker.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtils {

    public static String PLUGIN_NAME = "";

    public static String PLUGIN_LOGGER_NAME = PLUGIN_NAME;

    private static Logger logger = null;

    public static void init(final JavaPlugin plugin) {
        PLUGIN_NAME = plugin.getName();
        PLUGIN_LOGGER_NAME = plugin.getLogger().getName();
    }

    public static Logger getPluginLogger() {
        if (logger == null) {
            if(!PLUGIN_LOGGER_NAME.isEmpty()) {
                logger = LoggerFactory.getLogger(PLUGIN_LOGGER_NAME);
            } else {
                var stack = Thread.currentThread().getStackTrace();
                // [0] is getStackTrace, [1] is getPluginLogger, [2] is the caller
                var name = stack[2].getClassName();
                logger = LoggerFactory.getLogger(name);
                logger.warn("Uninitialized PLUGIN_LOGGER_NAME, using default logger \"{}\"", name);
            }
        }
        return logger;
    }
}
