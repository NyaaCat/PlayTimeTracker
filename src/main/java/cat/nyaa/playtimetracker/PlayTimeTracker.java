package cat.nyaa.playtimetracker;

import cat.nyaa.ecore.EconomyCore;
import cat.nyaa.playtimetracker.command.CommandHandler;
import cat.nyaa.playtimetracker.condition.ICondition;
import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.executor.ITaskExecutor;
import cat.nyaa.playtimetracker.executor.TaskExecutor;
import cat.nyaa.playtimetracker.listener.ListenerManager;
import cat.nyaa.playtimetracker.reward.IEconomyCoreProvider;
import cat.nyaa.playtimetracker.task.PTTTaskManager;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import cat.nyaa.playtimetracker.utils.PlaceholderAPIUtils;
import cat.nyaa.playtimetracker.workflow.IEssentialsAPIProvider;
import cat.nyaa.playtimetracker.workflow.LimitedTimeTrackerModel;
import net.ess3.api.IEssentials;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public final class PlayTimeTracker extends JavaPlugin implements IEconomyCoreProvider, IEssentialsAPIProvider {
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
    private PlayerRewardManager rewardManager;
    @Nullable
    private PlayerMissionManager missionManager;
    @Nullable
    private Plugin essentialsPlugin;
    @Nullable
    private EconomyCore economyCore;
    private ZoneId timezone;

    private TaskExecutor taskExecutor;
    private PlayTimeTrackerController controller;
    private Listener listener;


    @Nullable
    public static PlayTimeTracker getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {

        LoggerUtils.init(this);

        instance = this;
        this.pttConfiguration = new PTTConfiguration(this);
        pttConfiguration.load();
        try {
            final long precision = 1000;
            var validationContext = new MissionData.IConditionCompiler() {

                @Override
                public ICondition<?> compile(String expression) throws Exception {
                    return LimitedTimeTrackerModel.compileCondition(expression, precision);
                }
            };
            pttConfiguration.validate(validationContext);
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to compile mission conditions", e);
            return;
        }

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
        // Ecore
        var economyProvider = this.getServer().getServicesManager().getRegistration(EconomyCore.class);
        if (economyProvider != null) {
            this.economyCore = economyProvider.getProvider();
        }

        //command
        this.commandHandler = new CommandHandler(this, i18n);
        PluginCommand mainCommand = getCommand("playtimetracker");
        if (mainCommand != null) {
            mainCommand.setExecutor(commandHandler);
            mainCommand.setTabCompleter(commandHandler);
        } else {
            throw new RuntimeException("Command registration failed");
        }

        taskExecutor = new TaskExecutor(this, 10, 1, TimeUnit.SECONDS);
        var timeBuilder = new PiecewiseTimeInfo.Builder(timezone, DayOfWeek.MONDAY);
        controller = new PlayTimeTrackerController(this, taskExecutor, pttConfiguration.missionConfig, databaseManager, timeBuilder);
        taskExecutor.start();
        listener = new PlayTimeTrackerController.PTTListener(controller);
        getServer().getPluginManager().registerEvents(listener, this);

        //listener
        //this.listenerManager = new ListenerManager(this);
        //task
        //this.taskManager = new PTTTaskManager(this, pttConfiguration);
        //afk
        this.afkManager = new PlayerAFKManager(this);
        //record
        //this.timeRecordManager = new TimeRecordManager(this, databaseManager.getTimeTrackerConnection());
        //reward
        //this.rewardManager = new PlayerRewardManager(this, databaseManager.getRewardsConnection());
        //Mission
        //this.missionManager = new PlayerMissionManager(this, pttConfiguration, timeRecordManager, rewardManager, databaseManager.getCompletedMissionConnection());
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
    public PlayerRewardManager getRewardManager() {
        return rewardManager;
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

    @Override
    @Nullable
    public EconomyCore getEconomyCore() {
        return economyCore;
    }

    @Override
    public @Nullable IEssentials getEssentialsAPI() {
        // can ensure that essentialsPlugin is IEssentials
        return (IEssentials) essentialsPlugin;
    }

    @Nullable
    public I18n getI18n() {
        return i18n;
    }

    @Override
    public void onDisable() {

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }

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
        if (controller != null) {
            try {
                controller.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            controller = null;
        }
        if (taskExecutor != null) {
            taskExecutor.stop();
            taskExecutor = null;
        }
    }

    public void onReload() {
        onDisable();
        onEnable();
    }
}
