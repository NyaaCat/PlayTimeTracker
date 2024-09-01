package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.playtimetracker.db.tables.RewardsTable;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

public class RewardsConnection {
    private final RewardsTable rewardsTable;
    private final Plugin plugin;
    
    public RewardsConnection(HikariDataSource ds, Plugin plugin) {
        this.plugin = plugin;
        this.rewardsTable = new RewardsTable(ds, plugin.getSLF4JLogger());
        this.rewardsTable.tryCreateTable(plugin);
    }

    public void close() {


    }

    // TODO: just for consistency; this should be remove in the future
    public RewardsTable getRewardsTable() {
        return rewardsTable;
    }
}
