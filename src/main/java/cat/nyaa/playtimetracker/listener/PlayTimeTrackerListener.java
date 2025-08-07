package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.IPlayTimeTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayTimeTrackerListener implements Listener {

    private final IPlayTimeTracker provider;

    public PlayTimeTrackerListener(IPlayTimeTracker IProvider) {
        this.provider = IProvider;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var controller = this.provider.getController();
        if (controller == null) {
            return;
        }
        var player = event.getPlayer();
        controller.login(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        var controller = this.provider.getController();
        if (controller == null) {
            return;
        }
        var player = event.getPlayer();
        controller.logout(player);
    }
}