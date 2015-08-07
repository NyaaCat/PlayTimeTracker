package cat.nyaa.playtimetracker;

import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Runnable, Listener {
    private FileConfiguration cfg;
    private RecordMgr recordMgr;

    private Map<String, Rule> rules;
    private Map<String, Reward> rewardMap;

    public Map<String, Rule> getRules() {
        return rules;
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        recordMgr.updateOnline();
        recordMgr.save();
    }

    @Override
    public void onEnable() {
        getCommand("playtimetracker").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        reloadConfig();
        cfg = getConfig();

        rewardMap = new HashMap<>();
        for (String i : cfg.getConfigurationSection("rewards").getValues(false).keySet()) {
            rewardMap.put(i, new Reward(cfg.getConfigurationSection("rewards." + i)));
        }
        rules = new HashMap<>();
        for (String n : cfg.getConfigurationSection("rules").getValues(false).keySet()) {
            rules.put(n, new Rule(n, cfg.getConfigurationSection("rules." + n)));
        }

        recordMgr = new RecordMgr(this);
        recordMgr.load();
        for (Player p : Bukkit.getOnlinePlayers()) {
            onPlayerJoin(new PlayerJoinEvent(p, null));
        }

        Plugin p = getServer().getPluginManager().getPlugin("Essentials");
        if (p instanceof IEssentials) {
            ess = (IEssentials) p;
        } else {
            getLogger().warning("Essential not exists, afk setting will be ignored.");
            ess = null;
        }

        Bukkit.getScheduler().runTaskTimer(this, this, cfg.getLong("save-interval") * 20L, cfg.getLong("save-interval") * 20L);
    }

    private IEssentials ess = null;

    public boolean isAFK(UUID id) {
        if (!cfg.getBoolean("ignore-afk") || ess == null || id == null) return false;
        else return ess.getUser(id).isAfk();
    }

    private void printStatistic(CommandSender s, OfflinePlayer p) {
        recordMgr.printStatistic(s, p);
    }

    private void notifyReward(Player player, Collection<Rule> satisfiedRuleList) {
        if (satisfiedRuleList.size() == 0) return;
        String str = "";
        for (Rule s : satisfiedRuleList) {
            str += ", " + s.name;
        }
        ;
        player.sendMessage("You have the following rewards to redeem: " + str.substring(2));
    }

    private void applyReward(Rule rule, Player p) {
        rewardMap.get(rule.reward).applyTo(p);
    }

    @Override
    public void run() { // Save interval
        recordMgr.updateOnline();
        for (Player p : Bukkit.getOnlinePlayers()) {
            notifyAcquire(p);
        }
        recordMgr.save();
    }


    private void notifyAcquire(Player p) {
        Set<Rule> satisfiedRules = recordMgr.getSatisfiedRules(p.getUniqueId());
        Set<Rule> unacquired = new HashSet<>();
        for (Rule r : satisfiedRules) {
            if (r.autoGive) {
                applyReward(r, p);
                recordMgr.setRuleAcquired(p.getUniqueId(), r);
            } else {
                unacquired.add(r);
            }
        }
        if (unacquired.size() > 0) {
            notifyReward(p, unacquired);
        }
        recordMgr.save();
    }

    private void acquireAll(Player p) {
        Set<Rule> satisfiedRules = recordMgr.getSatisfiedRules(p.getUniqueId());
        for (Rule r : satisfiedRules) {
            applyReward(r, p);
            recordMgr.setRuleAcquired(p.getUniqueId(), r);
        }
        recordMgr.save();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        recordMgr.sessionStart(id, rules.values());
        recordMgr.updateSingle(event.getPlayer(), false);
        notifyAcquire(event.getPlayer());
    }

    @EventHandler
    public void onPlayerExit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        recordMgr.sessionEnd(id);
        recordMgr.updateSingle(event.getPlayer());
        recordMgr.save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only player can do this");
            } else if (sender.hasPermission("ptt.view")) {
                recordMgr.updateSingle((Player) sender);
                printStatistic(sender, (Player) sender);
            } else {
                sender.sendMessage("You have no permission to do this");
            }
            return true;
        } else if ("reload".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("ptt.reload")) {
                sender.sendMessage("Reloading...");
                onDisable();
                onEnable();
            } else {
                sender.sendMessage("You have no permission to do this");
            }
            return true;
        } else if ("reset".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("ptt.reset")) {
                if (args.length <= 1) return false;
                String name = args[1];
                if ("all".equalsIgnoreCase(name)) {
                    recordMgr.resetAll();
                } else {
                    recordMgr.reset(Bukkit.getOfflinePlayer(name).getUniqueId());
                }
                recordMgr.save();
                sender.sendMessage("Executed.");
            } else {
                sender.sendMessage("You have no permission to do this");
            }
            return true;
        } else if ("acquire".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only player can do this");
                return true;
            }
            acquireAll((Player) sender);
            return true;
        } else {
            if (sender.hasPermission("ptt.view.others")) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);
                if (p instanceof Player) {
                    recordMgr.updateSingle((Player) p);
                }
                printStatistic(sender, p);
            } else {
                sender.sendMessage("You have no permission to do this");
            }
            return true;
        }
    }
}
