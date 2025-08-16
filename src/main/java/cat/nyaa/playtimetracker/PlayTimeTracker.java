package cat.nyaa.playtimetracker;

import cat.nyaa.ecore.EconomyCore;
import cat.nyaa.playtimetracker.command.CommandHandler;
import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.executor.TaskExecutor;
import cat.nyaa.playtimetracker.listener.EssAfkListener;
import cat.nyaa.playtimetracker.listener.ListenerManager;
import cat.nyaa.playtimetracker.listener.PlayTimeTrackerListener;
import cat.nyaa.playtimetracker.reward.IEconomyCoreProvider;
import cat.nyaa.playtimetracker.task.PTTTaskManager;
import cat.nyaa.playtimetracker.utils.LoggerUtils;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import cat.nyaa.playtimetracker.utils.PlaceholderAPIUtils;
import cat.nyaa.playtimetracker.workflow.IEssentialsAPIProvider;
import cat.nyaa.playtimetracker.workflow.LimitedTimeTrackerModel;
import net.ess3.api.IEssentials;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

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
    private Collection<Listener> listeners;

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
            MissionData.IConditionCompiler validationContext = (String expression) -> LimitedTimeTrackerModel.compileCondition(expression, precision);
            pttConfiguration.validate(validationContext);
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to compile mission conditions", e);
            return;
        }
        pttConfiguration.save();

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

        //listener
        //this.listenerManager = new ListenerManager(this);
        //task
        //this.taskManager = new PTTTaskManager(this, pttConfiguration);
        //afk
        this.afkManager = new PlayerAFKManager(this.getPttConfiguration(), this.getEssentialsAPI());
        this.afkManager.registerCheckTask(this);
        if (!useEssentialsAFK()) {
            this.afkManager.setAFKStateChangeCallback(this::onAFKStateChange);
        }
        PlayerAFKManager.setInstance(this.afkManager);
        //record
        //this.timeRecordManager = new TimeRecordManager(this, databaseManager.getTimeTrackerConnection());
        //reward
        //this.rewardManager = new PlayerRewardManager(this, databaseManager.getRewardsConnection());
        //Mission
        //this.missionManager = new PlayerMissionManager(this, pttConfiguration, timeRecordManager, rewardManager, databaseManager.getCompletedMissionConnection());

        taskExecutor = new TaskExecutor(this, pttConfiguration.syncIntervalTick, pttConfiguration.timerIntervalMS, TimeUnit.MILLISECONDS);
        var timeBuilder = new PiecewiseTimeInfo.Builder(timezone, DayOfWeek.MONDAY);
        controller = new PlayTimeTrackerController(
                this,
                taskExecutor,
                pttConfiguration.missionConfig,
                databaseManager,
                timeBuilder,
                useEssentialsAFK() ? this::checkAFKEss : afkManager::getSelfHostedAfkState
        );
        taskExecutor.start();

        Supplier<@Nullable PlayTimeTrackerController> supplier = this::getController;
        listeners = new ArrayList<>();
        listeners.add(new PlayTimeTrackerListener(supplier));
        var ess = this.getEssentialsAPI();
        if (ess != null) {
            listeners.add(new EssAfkListener(supplier));
        }
        var pluginManager = getServer().getPluginManager();
        for (Listener listener : listeners) {
            pluginManager.registerEvents(listener, this);
        }
    }

    private boolean useEssentialsAFK() {
        if (this.pttConfiguration == null) {
            return false;
        }
        return this.pttConfiguration.useEssAfkStatus && this.essentialsPlugin != null;
    }

    private boolean checkAFKEss(UUID playerId) {
        var ess = this.getEssentialsAPI();
        assert ess != null;
        return ess.getUser(playerId).isAfk();
    }

    private void onAFKStateChange(Player player, boolean isAFK) {
        if (this.controller != null) {
            if (isAFK) {
                this.getLogger().log(Level.INFO,"Player {0} is now AFK", player.getName());
                this.controller.awayFromKeyboard(player);
            } else {
                this.getLogger().log(Level.INFO, "Player {0} is no longer AFK", player.getName());
                this.controller.backToKeyboard(player);
            }
        }
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
    public PlayTimeTrackerController getController() {
        return this.controller;
    }

    @Nullable
    public PlayerAFKManager getPlayerAFKManager() {
        return this.afkManager;
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
    @Nullable
    public IEssentials getEssentialsAPI() {
        // can ensure that essentialsPlugin is IEssentials
        return (IEssentials) essentialsPlugin;
    }

    @Nullable
    public I18n getI18n() {
        return i18n;
    }

    @Override
    public void onDisable() {

        if (listeners != null) {
            for (var listener : listeners) {
                HandlerList.unregisterAll(listener);
            }
            listeners = null;
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

        if (this.afkManager != null) {
            this.afkManager.unregisterCheckTask();
            this.afkManager = null;
        }
        PlayerAFKManager.setInstance(null);
    }

    public void onReload() {
        onDisable();
        onEnable();
    }
}
