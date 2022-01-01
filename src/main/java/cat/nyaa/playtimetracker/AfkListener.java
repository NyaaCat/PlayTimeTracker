package cat.nyaa.playtimetracker;

import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static cat.nyaa.playtimetracker.AfkListener.setAfk;

public class AfkListener extends BukkitRunnable implements Listener {
    private static final Map<UUID, Vector> lastDirection = new HashMap<>();
    private static final Map<UUID, Long> lastActivity = new HashMap<>();
    private static final Set<UUID> afkPlayers = new HashSet<>();
    public static boolean checkAfk;
    public static int afkTime;
    //private final PTT plugin;
    private final boolean cancelAfkOnChat;
    private final boolean cancelAfkOnCommand;
    private final boolean cancelAfkOnMove;

    public AfkListener(PTT plugin) {
        checkAfk = plugin.cfg.getBoolean("check-afk", true);
        afkTime = plugin.cfg.getInt("afk-time", 180);
        cancelAfkOnChat = plugin.cfg.getBoolean("cancel-afk-on.chat", true);
        cancelAfkOnCommand = plugin.cfg.getBoolean("cancel-afk-on.command", true);
        cancelAfkOnMove = plugin.cfg.getBoolean("cancel-afk-on.move", true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.runTaskTimer(plugin, 20, 20L * plugin.cfg.getInt("afk-check-interval", 30));
        if (plugin.ess != null) {
            new EssentialsAfkListener(plugin);
        }
    }

    public static void updateActivity(UUID uuid) {
        lastActivity.put(uuid, System.currentTimeMillis());
        afkPlayers.remove(uuid);
    }

    public static boolean isAfk(UUID id) {
        return afkPlayers.contains(id);
    }

    public static void setAfk(UUID uuid, boolean isAfk) {
        if (!isAfk) {
            afkPlayers.remove(uuid);
        } else {
            afkPlayers.add(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        updateActivity(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastActivity.remove(event.getPlayer().getUniqueId());
        afkPlayers.remove(event.getPlayer().getUniqueId());
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(PlayerChatEvent event) {
        if (checkAfk && cancelAfkOnChat) {
            updateActivity(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (checkAfk && cancelAfkOnCommand) {
            updateActivity(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (checkAfk && cancelAfkOnMove) {
            Player p = event.getPlayer();
            UUID id = p.getUniqueId();
            Vector direction = p.getEyeLocation().getDirection();
            if (!(lastDirection.containsKey(id) && direction.equals(lastDirection.get(id)))) {
                lastDirection.put(id, direction.clone());
                updateActivity(id);
            }
        }
    }

    @Override
    public void run() {
        if (checkAfk) {
            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();
                if (!isAfk(id)) {
                    Long last = lastActivity.get(id);
                    if (last != null && now - last >= afkTime * 1000L) {
                        setAfk(id, true);
                    }
                }
            }
        }
    }
}

class EssentialsAfkListener implements Listener {
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
