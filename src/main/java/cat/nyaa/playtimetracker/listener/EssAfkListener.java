package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.IPlayTimeTracker;
import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class EssAfkListener implements Listener {

    private final IPlayTimeTracker provider;

    public EssAfkListener(IPlayTimeTracker provider) {
        this.provider = provider;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAfkChange(AfkStatusChangeEvent event){
        var user = event.getAffected();
        var status = event.getValue();
        var player = user.getBase();
        var controller = this.provider.getController();
        if (controller != null) {

        }
    }
}
