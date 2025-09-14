package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PlayTimeTrackerController;
import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EssAfkListener implements Listener {

    private final PlayTimeTrackerController controller;

    public EssAfkListener(PlayTimeTrackerController controller) {
        this.controller = controller;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAfkChange(AfkStatusChangeEvent event){
        var user = event.getAffected();
        var status = event.getValue();
        var player = user.getBase();
        if (status) {
            this.controller.awayFromKeyboard(player);
        } else {
            this.controller.backToKeyboard(player);
        }
    }
}
