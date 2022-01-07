package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PTT;
import cat.nyaa.playtimetracker.RecordManager;
import cat.nyaa.playtimetracker.Rule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;

public class PTTListener implements Listener {
    private final PTT plugin;

    public PTTListener(PTT plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        // afkRevive reward automatically applied
        RecordManager.SessionedRecord rec = plugin.getUpdater().getFullRecord(id);
        if (rec != null && rec.dbRec != null && rec.dbRec.lastSeen != null) {
            ZonedDateTime lastSeen = rec.dbRec.lastSeen;
            ZonedDateTime now = ZonedDateTime.now();
            Duration gap = Duration.between(lastSeen, now);
            for (Rule rule : plugin.getRules().values()) {
                if (rule.period == Rule.PeriodType.LONGTIMENOSEE &&
                        Duration.of(rule.require, DAYS).minus(gap).isNegative() &&
                        plugin.inGroup(id, rule.group)) {
                    plugin.applyReward(rule, event.getPlayer());
                }
            }
        }

        plugin.getUpdater().sessionStart(id);
        if (plugin.pttConfiguration.DisplayOnLogin) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.printStatistic(event.getPlayer(), event.getPlayer());
                    plugin.notifyAcquire(event.getPlayer());
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    @EventHandler
    public void onPlayerExit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        plugin.getUpdater().sessionEnd(id);
    }
}
