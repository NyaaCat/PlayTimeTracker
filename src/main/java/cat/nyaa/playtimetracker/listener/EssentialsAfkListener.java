package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PTT;
import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static cat.nyaa.playtimetracker.listener.AfkListener.setAfk;

public class EssentialsAfkListener implements Listener {
    public EssentialsAfkListener(PTT pl) {
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    @EventHandler
    public void onAfkStatusChange(AfkStatusChangeEvent event) {
        boolean isAfk = event.getValue();
        if (isAfk) {
            setAfk(event.getAffected().getBase().getUniqueId(), true);
        }
    }
}
