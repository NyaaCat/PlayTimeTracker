package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PlayTimeTrackerController;
import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class EssAfkListener implements Listener {

    private final Supplier<@Nullable PlayTimeTrackerController> provider;

    public EssAfkListener(Supplier<@Nullable PlayTimeTrackerController> provider) {
        this.provider = provider;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAfkChange(AfkStatusChangeEvent event){
        var user = event.getAffected();
        var status = event.getValue();
        var player = user.getBase();
        var controller = this.provider.get();
        if (controller != null && player.isConnected()) {
            if (status) {
                controller.awayFromKeyboard(player);
            } else {
                controller.backToKeyboard(player);
            }
        }
    }
}
