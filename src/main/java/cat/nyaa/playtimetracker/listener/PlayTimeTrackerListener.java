package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PlayTimeTrackerController;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class PlayTimeTrackerListener implements Listener {

    private final Supplier<@Nullable PlayTimeTrackerController> provider;

    public PlayTimeTrackerListener(Supplier<@Nullable PlayTimeTrackerController> provider) {
        this.provider = provider;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var controller = this.provider.get();
        if (controller == null) {
            return;
        }
        var player = event.getPlayer();
        controller.login(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        var controller = this.provider.get();
        if (controller == null) {
            return;
        }
        var player = event.getPlayer();
        controller.logout(player);
    }
}