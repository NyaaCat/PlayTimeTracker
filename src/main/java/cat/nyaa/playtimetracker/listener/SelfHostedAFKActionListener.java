package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PlayerAFKManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SelfHostedAFKActionListener implements Listener {

    private final PlayerAFKManager afkManager;

    public SelfHostedAFKActionListener(PlayerAFKManager afkManager) {
        this.afkManager = afkManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        this.afkManager.addPlayer(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        this.afkManager.removePlayer(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getPitch() != to.getPitch() || from.getYaw() != to.getYaw()) {
            this.afkManager.playerVisionChange(event);
        }
    }
}
