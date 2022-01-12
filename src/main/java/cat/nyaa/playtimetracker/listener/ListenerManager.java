package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PlayTimeTracker;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class ListenerManager {
    public final List<Listener> listenerList = new ArrayList<>();

    public ListenerManager(PlayTimeTracker plugin) {
        listenerList.add(new PTTListener());

        listenerList.forEach(listener ->
                plugin.getServer().getPluginManager().registerEvents(listener, plugin)
        );
    }


    public void destructor() {
        listenerList.forEach(HandlerList::unregisterAll);
    }
}
