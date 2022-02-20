package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.command.CommandHandler;
import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.listener.ListenerManager;
import cat.nyaa.playtimetracker.task.PTTTaskManager;
import cat.nyaa.playtimetracker.utils.PlaceholderAPIUtils;
import net.ess3.api.IEssentials;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;

public final class PlayTimeTracker extends JavaPlugin {
    @Nullable
    private static PlayTimeTracker instance;
    @Nullable
    private PTTConfiguration pttConfiguration;
    @Nullable
    private I18n i18n;
    @Nullable
    private DatabaseManager databaseManager;
    @Nullable
    private CommandHandler commandHandler;
    @Nullable
    private PTTTaskManager taskManager;
    @Nullable
    private ListenerManager listenerManager;
    @Nullable
    private PlayerAFKManager afkManager;
    @Nullable
    private TimeRecordManager timeRecordManager;
    @Nullable
    private PlayerMissionManager missionManager;
    @Nullable
    private Plugin essentialsPlugin;
    private ZoneId timezone;


    @Nullable
    public static PlayTimeTracker getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.pttConfiguration = new PTTConfiguration(this);
        pttConfiguration.load();
        this.timezone = ZoneId.of(pttConfiguration.timezone);
        this.i18n = new I18n(this, pttConfiguration.language);
        i18n.load();

        //db
        this.databaseManager = new DatabaseManager(pttConfiguration.databaseConfig);
        // papi
        PlaceholderAPIUtils.init();
        // Essential Hook
        Plugin essentials = getServer().getPluginManager().getPlugin("Essentials");
        if (essentials != null) {
            if (essentials instanceof IEssentials) {
                this.essentialsPlugin = essentials;
            }
        }
        if (this.essentialsPlugin == null) {
            getLogger().warning("Essential not exists, afk setting will be ignored.");
            this.essentialsPlugin = null;
        }
        //command
        this.commandHandler = new CommandHandler(this, i18n);
        PluginCommand mainCommand = getCommand("playtimetracker");
        if (mainCommand != null) {
            mainCommand.setExecutor(commandHandler);
            mainCommand.setTabCompleter(commandHandler);
        } else throw new RuntimeException("Command registration failed");
        //listener
        this.listenerManager = new ListenerManager(this);
        //task
        this.taskManager = new PTTTaskManager(this, pttConfiguration);
        //afk
        this.afkManager = new PlayerAFKManager(this);
        //record
        this.timeRecordManager = new TimeRecordManager(this, databaseManager.getTimeTrackerConnection());
        //Mission
        this.missionManager = new PlayerMissionManager(this, pttConfiguration, timeRecordManager, databaseManager.getCompletedMissionConnection());
    }

    @Nullable
    public ZoneId getTimezone() {
        return timezone;
    }

    @Nullable
    public Plugin getEssentialsPlugin() {
        return essentialsPlugin;
    }

    @Nullable
    public PlayerAFKManager getAfkManager() {
        return afkManager;
    }

    @Nullable
    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    @Nullable
    public TimeRecordManager getTimeRecordManager() {
        return timeRecordManager;
    }

    @Nullable
    public PlayerMissionManager getMissionManager() {
        return missionManager;
    }

    @Nullable
    public PTTConfiguration getPttConfiguration() {
        return pttConfiguration;
    }

    @Nullable
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Nullable
    public I18n getI18n() {
        return i18n;
    }

    @Override
    public void onDisable() {
        if (this.listenerManager != null) {
            this.listenerManager.destructor();
            this.listenerManager = null;
        }
        if (this.taskManager != null) {
            this.taskManager.destructor();
            this.taskManager = null;
        }

        if (databaseManager != null) {
            databaseManager.close();
            databaseManager = null;
        }
        // Plugin shutdown logic
    }

    public void onReload() {
        onDisable();
        onEnable();
    }
}
