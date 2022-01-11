package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class PlayTimeTracker extends JavaPlugin {
    @Nullable
    private PlayTimeTracker instance;
    @Nullable
    private PTTConfiguration pttConfiguration;
    @Nullable
    private I18n i18n;
    @Nullable
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;
        this.pttConfiguration = new PTTConfiguration(this);
        pttConfiguration.load();
        this.i18n = new I18n(this, pttConfiguration.language);
        i18n.load();
        this.databaseManager = new DatabaseManager(pttConfiguration.databaseConfig);
    }

    @Nullable
    public PlayTimeTracker getInstance() {
        return instance;
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
        Bukkit.getScheduler().cancelTasks(this);

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
