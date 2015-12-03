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
import org.bukkit.scheduler.BukkitRunnable;

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
        Locale.init(cfg.getConfigurationSection("message"));

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

    public boolean isAFK(String playerName) {
        if (!cfg.getBoolean("ignore-afk") || ess == null || playerName == null) return false;
        else return ess.getUser(playerName).isAfk();
    }

    public boolean inGroup(String id, Set<String> group) {
        if (ess == null || group == null || id == null) return true;
        return group.contains(ess.getUser(id).getGroup());
    }

    private void printStatistic(CommandSender s, OfflinePlayer p) {
        s.sendMessage(Locale.get("statistic-for", p.getName()));
        recordMgr.printStatistic(s, p);
    }

    private void notifyReward(Player player, Collection<Rule> satisfiedRuleList) {
        if (satisfiedRuleList.size() == 0) return;
        player.sendMessage(Locale.get("have-reward-redeem"));
        for (Rule s : satisfiedRuleList) {
            player.sendMessage(Locale.get("have-reward-redeem-format", s.name));
        }
    }

    private void applyReward(Rule rule, Player p) {
        Reward reward = rewardMap.get(rule.reward);
        reward.applyTo(p);
        p.sendMessage(Locale.get("rule-applied", rule.name));
        if (reward.getDescription() != null && reward.getDescription().length() > 0) {
            p.sendMessage(rewardMap.get(rule.reward).getDescription());
        }
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
        Set<Rule> satisfiedRules = recordMgr.getSatisfiedRules(p.getName());
        Set<Rule> unacquired = new HashSet<>();
        for (Rule r : satisfiedRules) {
            if (r.autoGive) {
                applyReward(r, p);
                recordMgr.setRuleAcquired(p.getName(), r);
            } else {
                unacquired.add(r);
            }
        }
        if (unacquired.size() > 0) {
            notifyReward(p, unacquired);
        }
        recordMgr.save();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String id = event.getPlayer().getName();
        recordMgr.sessionStart(id, rules.values());
        recordMgr.updateSingle(event.getPlayer(), false);
        if (cfg.getBoolean("display-on-login")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    printStatistic(event.getPlayer(), event.getPlayer());
                    notifyAcquire(event.getPlayer());
                }
            }.runTaskLater(this, 20L);
        }
    }

    @EventHandler
    public void onPlayerExit(PlayerQuitEvent event) {
        String id = event.getPlayer().getName();
        recordMgr.sessionEnd(id);
        recordMgr.updateSingle(event.getPlayer());
        recordMgr.save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Locale.get("only-player-can-do"));
            } else if (sender.hasPermission("ptt.view")) {
                recordMgr.updateSingle((Player) sender);
                printStatistic(sender, (Player) sender);
            } else {
                sender.sendMessage(Locale.get("no-permission"));
            }
            return true;
        } else if ("reload".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("ptt.reload")) {
                onDisable();
                onEnable();
                sender.sendMessage(Locale.get("reload-finished"));
            } else {
                sender.sendMessage(Locale.get("no-permission"));
            }
            return true;
        } else if ("reset".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("ptt.reset")) {
                if (args.length <= 1) return false;
                String name = args[1];
                if ("all".equalsIgnoreCase(name)) {
                    recordMgr.resetAll();
                } else {
                    recordMgr.reset(name);
                }
                recordMgr.save();
                sender.sendMessage(Locale.get("command-done"));
            } else {
                sender.sendMessage(Locale.get("no-permission"));
            }
            return true;
        } else if ("acquire".equalsIgnoreCase(args[0]) || "ac".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("ptt.acquire")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Locale.get("only-player-can-do"));
                }
                Set<Rule> satisfiedRules = recordMgr.getSatisfiedRules(((Player) sender).getName());
                if (satisfiedRules.size() == 0) {
                    sender.sendMessage(Locale.get("nothing-to-acquire"));
                    return true;
                }
                breakpoint:
                if (args.length <= 1) { // acquire all
                    for (Rule r : satisfiedRules) {
                        applyReward(r, (Player) sender);
                        recordMgr.setRuleAcquired(((Player) sender).getName(), r);
                    }
                    recordMgr.save();
                } else {
                    if (!rules.containsKey(args[1])) {
                        sender.sendMessage(Locale.get("no-such-rule"));
                        return true;
                    }
                    for (Rule r : satisfiedRules) {
                        if (r.name.equalsIgnoreCase(args[1])) {
                            applyReward(r, (Player) sender);
                            recordMgr.setRuleAcquired(((Player) sender).getName(), r);
                            recordMgr.save();
                            break breakpoint;
                        }
                    }
                    sender.sendMessage(Locale.get("cannot-acquire", args[1]));
                }
            } else {
                sender.sendMessage(Locale.get("no-permission"));
            }
            return true;
        } else if ("help".equalsIgnoreCase(args[0])) {
            return false;
        } else {
            if (sender.hasPermission("ptt.view.others")) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);
                if (p instanceof Player) {
                    recordMgr.updateSingle((Player) p);
                }
                printStatistic(sender, p);
            } else {
                sender.sendMessage(Locale.get("no-permission"));
            }
            return true;
        }
    }
}

