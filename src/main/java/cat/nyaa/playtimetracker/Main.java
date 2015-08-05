package cat.nyaa.playtimetracker;

import javafx.util.Pair;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Runnable, Listener {
    public FileConfiguration cfg;
    public Map<String, OnlineRecord> map;
    private Map<String, Rule> rules;
    private Map<Player, Pair<Long, Long>> session = new HashMap<>();
    private Map<Player, Pair<Long, List<Rule>>> longTimeNoSeeMap = new HashMap<>();
    private Map<String, Reward> rewardMap;
    private Map<Player, String> sessionComplete;

    @Override
    public List<Class<?>> getDatabaseClasses() {
        return Collections.singletonList(OnlineRecord.class);
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            OnlineRecord r;
            if (!map.containsKey(id.toString())) {
                r = OnlineRecord.createFor(id);
                map.put(id.toString(), r);
                getDatabase().insert(r);
            } else {
                r = map.get(id.toString());
                r.update(true); //TODO afk
                getDatabase().update(r);
            }
            notifyReward(p, checkReward(r));
        }
    }

    @Override
    public void onEnable() {
        getCommand("playtimetracker").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        cfg = getConfig();

        rewardMap = new HashMap<>();
        for (String i : cfg.getConfigurationSection("rewards").getValues(false).keySet()) {
            rewardMap.put(i, new Reward(cfg.getConfigurationSection("rewards." + i)));
        }
        rules = new HashMap<>();
        for (String n : cfg.getConfigurationSection("rules").getValues(false).keySet()) {
            rules.put(n, new Rule(n, cfg.getConfigurationSection("rules." + n)));
        }

        map = new HashMap<>();
        for (OnlineRecord r : getDatabase().find(OnlineRecord.class).findList()) {
            map.put(r.getUuid(), r);
        }

        sessionComplete = new HashMap<>();
    }

    private void printStatistic(CommandSender s, OfflinePlayer p) {
        OnlineRecord r = map.get(p.getUniqueId().toString());
        if (r == null) s.sendMessage("No record for that player");
        else {
            s.sendMessage("Statistic for player: " + p.getName());
            s.sendMessage(String.format("Total online: %f sec", r.getTotalTime() / 1000.0));
            s.sendMessage(String.format("Month online: %f sec", r.getMonthTime() / 1000.0));
            s.sendMessage(String.format("Week  online: %f sec", r.getWeekTime() / 1000.0));
            s.sendMessage(String.format("Day   online: %f sec", r.getDayTime() / 1000.0));
            if (p.isOnline()) {
                s.sendMessage(String.format("Sessi online: %f sec", session.get(p) == null ? 0.0 : (session.get(p).getValue() / 1000)));
            }
        }
    }

    private List<String> checkReward(OnlineRecord onlineRecord) {
        List<String> ret = new LinkedList<>();
        for (Rule rule : rules.values()) {
            String ruleName = rule.name;
            switch (rule.period) {
                case DAY: {
                    long L = rule.require * 1000 * 60, H = L + rule.timeout * 1000 * 60;
                    if (!onlineRecord.isCompleted(Rule.PeriodType.DAY, ruleName) && onlineRecord.getDayTime() >= L && onlineRecord.getDayTime() <= H) {
                        if (rule.autoGive) applyReward(rule, onlineRecord.getUuid());
                        ret.add(ruleName);
                    }

                }
                case MONTH: {
                    long L = rule.require * 1000 * 60, H = L + rule.timeout * 1000 * 60;
                    if (!onlineRecord.isCompleted(Rule.PeriodType.MONTH, ruleName) && onlineRecord.getMonthTime() >= L && onlineRecord.getMonthTime() <= H) {
                        if (rule.autoGive) applyReward(rule, onlineRecord.getUuid());
                        ret.add(ruleName);
                    }
                }
                case WEEK: {
                    long L = rule.require * 1000 * 60, H = L + rule.timeout * 1000 * 60;
                    if (!onlineRecord.isCompleted(Rule.PeriodType.WEEK, ruleName) && onlineRecord.getWeekTime() >= L && onlineRecord.getWeekTime() <= H) {
                        if (rule.autoGive) applyReward(rule, onlineRecord.getUuid());
                        ret.add(ruleName);
                    }
                }
            }
        }
        return ret;
    }

    private List<String> checkLongTimeNoSeeReward(OnlineRecord onlineRecord, Player player) {
        List<String> ret = new LinkedList<>();
        for (Rule rule : rules.values()) {
            if (rule.period == Rule.PeriodType.LONGTIMENOSEE) {
                String ruleName = rule.name;
                long nowtime = new Date().getTime();
                if (nowtime - onlineRecord.getLastSeen() >= rule.require * 24 * 60 * 60 * 1000L) {
                    if (rule.autoGive) applyReward(rule, player);
                    else {
                        if (!longTimeNoSeeMap.containsKey(player))
                            longTimeNoSeeMap.put(player, new Pair<>(nowtime, new ArrayList<>()));
                        longTimeNoSeeMap.get(player).getValue().add(rule);
                        ret.add(ruleName);
                    }
                }
            }
        }
        return ret;
    }

    private Boolean isSessionCompleted(Player p, String rule) {
        String a = sessionComplete.get(p);
        if (a == null) return false;
        return a.equals(rule) || a.startsWith(rule + ",") || a.endsWith("," + rule);
    }

    private void setSessionComplete(Player p, String rule) {
        if (isSessionCompleted(p, rule)) return;
        String a = sessionComplete.get(p);
        if (a == null) a = "";
        if ("".equals(a)) {
            a = rule;
        } else {
            a += "," + rule;
        }
        sessionComplete.put(p, a);
    }

    private List<String> checkSessionReward(Player player) {
        List<String> ret = new LinkedList<>();
        for (Rule rule : rules.values()) {
            if (rule.period == Rule.PeriodType.SESSION) {
                String ruleName = rule.name;
                if (session.get(player).getValue() >= (rule.require * 60 * 1000) && session.get(player).getValue() <= (rule.require + rule.timeout) * 60 * 1000 && !isSessionCompleted(player, ruleName)) {
                    if (rule.autoGive) applyReward(rule, player);
                    ret.add(ruleName);
                }
            }
        }
        return ret;
    }

    private void notifyReward(Player player, List<String> satisfiedRuleList) {
        if (satisfiedRuleList.size() == 0) return;
        String str = satisfiedRuleList.get(0);
        for (int i = 1; i < satisfiedRuleList.size(); i++)
            str += ", " + satisfiedRuleList.get(i);
        player.sendMessage("You have the following rewards to redeem: " + str);
    }

    private void applyReward(Rule rule, Player p) {
        rewardMap.get(rule.reward).applyTo(p);
        if (rule.period != Rule.PeriodType.SESSION && rule.period != Rule.PeriodType.LONGTIMENOSEE) {
            OnlineRecord r = map.get(p.getUniqueId().toString());
            r.setCompleted(rule.period, rule.name);
            getDatabase().update(r);
        } else if (rule.period == Rule.PeriodType.SESSION) {
            setSessionComplete(p, rule.name);
        }
    }

    private void applyReward(Rule rule, String uuid) {
        Player p = Bukkit.getPlayer(UUID.fromString(uuid));
        if (p != null) applyReward(rule, p);
    }

    @Override
    public void run() { // Save interval
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            OnlineRecord r;
            if (!map.containsKey(id.toString())) {
                r = OnlineRecord.createFor(id);
                map.put(id.toString(), r);
                getDatabase().insert(r);
            } else {
                r = map.get(id.toString());
                r.update(true);//TODO afk
                getDatabase().update(r);
            }
            notifyReward(p, checkReward(r));

            Long newtime = new Date().getTime();
            if (session.containsKey(p)) {
                Long oldtime = session.get(p).getKey();
                Long val = session.get(p).getValue();
                session.put(p, new Pair<>(newtime, val + newtime - oldtime)); //TODO afk
            } else {
                session.put(p, new Pair<>(new Date().getTime(), 0L));
            }
            notifyReward(p, checkSessionReward(p));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        session.put(event.getPlayer(), new Pair<>(new Date().getTime(), 0L));
        UUID id = event.getPlayer().getUniqueId();
        OnlineRecord r;
        if (!map.containsKey(id.toString())) {
            r = OnlineRecord.createFor(id);
            map.put(id.toString(), r);
            getDatabase().insert(r);
        } else {
            r = map.get(id.toString());
            notifyReward(event.getPlayer(), checkLongTimeNoSeeReward(r, event.getPlayer()));
            r.update(false);
            getDatabase().update(r);
        }
        notifyReward(event.getPlayer(), checkReward(r));
    }

    @EventHandler
    public void onPlayerExit(PlayerQuitEvent event) {
        session.remove(event.getPlayer());
        UUID id = event.getPlayer().getUniqueId();
        if (!map.containsKey(id.toString())) {
            OnlineRecord r = OnlineRecord.createFor(id);
            map.put(id.toString(), r);
            getDatabase().insert(r);
        } else {
            OnlineRecord r = map.get(id.toString());
            r.update(true); //TODO afk
            getDatabase().update(r);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only player can do this");
            } else if (sender.hasPermission("ptt.view")) {
                printStatistic(sender, (Player) sender);
            } else {
                sender.sendMessage("You have no permission to do this");
            }
            return true;
        } else if ("reload".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("ptt.reload")) {
                Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                    @Override
                    public void run() {
                        onDisable();
                    }
                }, 5L);

                Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                    @Override
                    public void run() {
                        onEnable();
                    }
                }, 15L);

                sender.sendMessage("Reloading...");
                return true;
            } else {
                sender.sendMessage("You have no permission to do this");
                return true;
            }
        } else if ("reset".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("ptt.reset")) {
                if (args.length <= 1) return false;
                String name = args[1];
                if ("all".equalsIgnoreCase(name)) {
                    getDatabase().delete(map.values());
                    map.clear();
                    sender.sendMessage("Command Executed.");
                    return true;
                } else {
                    String uuid = Bukkit.getOfflinePlayer(name).getUniqueId().toString();
                    if (!map.containsKey(uuid)) {
                        sender.sendMessage("No such player.");
                    } else {
                        getDatabase().delete(map.get(name));
                        map.remove(name);
                        sender.sendMessage("Command Executed.");
                    }
                    return true;
                }
            } else {
                sender.sendMessage("You have no permission to do this");
                return true;
            }
        } else if ("acquire".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only player can do this");
                return true;
            }
            Player p = (Player) sender;
            List<String> normalReward = checkReward(map.get(p.getUniqueId().toString()));
            List<String> sessionReward = checkSessionReward(p);
            for (String str : normalReward) applyReward(rules.get(str), p);
            for (String str : sessionReward) applyReward(rules.get(str), p);
            if (longTimeNoSeeMap.containsKey(p)) {
                long baseTime = longTimeNoSeeMap.get(p).getKey();
                long nowTime = new Date().getTime();
                for (Rule rule : longTimeNoSeeMap.get(p).getValue()) {
                    if (baseTime + rule.timeout * 60 * 1000 <= nowTime) {
                        applyReward(rule, p);
                    }
                }
                longTimeNoSeeMap.remove(p);
            }
            return true;
        } else {
            if (sender.hasPermission("ptt.view.others")) {
                printStatistic(sender, Bukkit.getOfflinePlayer(args[0]));
            } else {
                sender.sendMessage("You have no permission to do this");
            }
            return true;
        }
    }
}
