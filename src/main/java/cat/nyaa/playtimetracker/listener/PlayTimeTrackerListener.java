package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PlayTimeTrackerController;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayTimeTrackerListener implements Listener {

    private final PlayTimeTrackerController controller;

    public PlayTimeTrackerListener(PlayTimeTrackerController controller) {
        this.controller = controller;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        this.controller.login(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        this.controller.logout(player);
    }
}