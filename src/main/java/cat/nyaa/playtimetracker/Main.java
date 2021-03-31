package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.RecordManager.SessionedRecord;
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
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

public class Main extends JavaPlugin implements Runnable, Listener {
    private static Main instance;
    public FileConfiguration cfg; // main config file
    private DatabaseManager database;
    private RecordManager updater;

    private Map<String, Rule> rules;
    private Map<String, Reward> rewardMap;

    public static void log(String msg) {
        if (instance == null) {
            System.out.println("[PlayTimeTracker] " + msg);
        } else {
            instance.getLogger().info(msg);
        }
    }

    public static void debug(String msg) {
        if (instance != null) {
            instance.getLogger().log(Level.FINE, msg);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        updater.updateAllOnlinePlayers();
        database.synchronizeSave();
    }

    @Override
    public void onEnable() {
        // Basic config & events
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        cfg = getConfig();
        Locale.init(cfg.getConfigurationSection("message"));

        // Load Reward config
        rewardMap = new HashMap<>();
        for (String i : cfg.getConfigurationSection("rewards").getValues(false).keySet()) {
            rewardMap.put(i, new Reward(cfg.getConfigurationSection("rewards." + i)));
        }
        rules = new HashMap<>();
        for (String n : cfg.getConfigurationSection("rules").getValues(false).keySet()) {
            rules.put(n, new Rule(n, cfg.getConfigurationSection("rules." + n)));
        }

        // Database
        File legacyDataFile = new File(getDataFolder(), "data.txt");
        File newDataFile = new File(getDataFolder(), "database.yml");
        File recurrenceFile = new File(getDataFolder(), "recurrenceRules.yml");
        if (newDataFile.isFile()) {
            getLogger().info("Loading database... database.yml");
            if (legacyDataFile.isFile()) {
                getLogger().info("You can manually remove the old database: data.txt");
            }
            database = new DatabaseManager(newDataFile, recurrenceFile);
        } else {
            if (legacyDataFile.isFile()) { // migrate old db
                getLogger().info("Updating old database... data.txt");
                database = new DatabaseManager(newDataFile, legacyDataFile, recurrenceFile);
            } else { // no database
                getLogger().info("Creating database... database.yml");
                database = new DatabaseManager(newDataFile, recurrenceFile);
            }
        }

        // Essential Hook
        Plugin p = getServer().getPluginManager().getPlugin("Essentials");
        if (p instanceof IEssentials) {
            ess = (IEssentials) p;
        } else {
            getLogger().warning("Essential not exists, afk setting will be ignored.");
            ess = null;
        }

        // refresh online players
        updater = new RecordManager(database);
        for (Player player : getServer().getOnlinePlayers()) {
            updater.sessionStart(player.getUniqueId());
        }

        // Schedule event
        getCommand("playtimetracker").setExecutor(this);
        getCommand("playtimetracker").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this, cfg.getLong("save-interval") * 20L, cfg.getLong("save-interval") * 20L);
        new AfkListener(this);
    }

    // Essential Hooks
    public IEssentials ess = null;

    public static boolean isAFK(UUID id) {
        return instance != null && instance.cfg.getBoolean("check-afk") && instance._isAFK(id);
    }

    private boolean _isAFK(UUID id) {
        if (id == null) return false;
        if (cfg.getBoolean("use-ess-afk-status") && ess != null) {
            return ess.getUser(id).isAfk();
        }
        return AfkListener.checkAfk && AfkListener.isAfk(id);
    }

    private boolean inGroup(UUID id, Set<String> group) {
        if (ess == null || group == null || id == null) return true;
        return group.contains(ess.getUser(id).getGroup());
    }

    /**
     * naively apply the rule to the player
     * database untouched
     */
    private void applyReward(Rule rule, Player p) {
        Reward reward = rewardMap.get(rule.reward);
        reward.applyTo(p);
        p.sendMessage(Locale.get("rule-applied", rule.name));
        if (reward.getDescription() != null && reward.getDescription().length() > 0) {
            p.sendMessage(rewardMap.get(rule.reward).getDescription());
        }
        log(String.format("Reward rule %s applied to player %s", rule.name, p.getName()));
    }

    private void notifyAcquire(Player p) {
        Set<Rule> unacquired = getSatisfiedRules(p.getUniqueId()).stream()
                .filter(r -> { // apply all auto apply rule, leave unapplied
                    if (r.autoGive) {
                        applyReward(r, p);
                        updater.markRuleAsApplied(p.getUniqueId(), r);
                        return false;
                    } else {
                        return true;
                    }
                }).collect(Collectors.toSet());
        if (unacquired.size() > 0) {
            p.sendMessage(Locale.get("have-reward-redeem"));
            for (Rule s : unacquired) {
                p.sendMessage(Locale.get("have-reward-redeem-format", s.name));
            }
        }
    }

    @Override
    public void run() { // Auto-save timer
        debug("Auto-save timer executing...");
        updater.updateAllOnlinePlayers();
        for (Player p : Bukkit.getOnlinePlayers()) {
            notifyAcquire(p);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        // afkRevive reward automatically applied
        SessionedRecord rec = updater.getFullRecord(id);
        if (rec != null && rec.dbRec != null && rec.dbRec.lastSeen != null) {
            ZonedDateTime lastSeen = rec.dbRec.lastSeen;
            ZonedDateTime now = ZonedDateTime.now();
            Duration gap = Duration.between(lastSeen, now);
            for (Rule rule : rules.values()) {
                if (rule.period == Rule.PeriodType.LONGTIMENOSEE &&
                        Duration.of(rule.require, DAYS).minus(gap).isNegative() &&
                        inGroup(id, rule.group)) {
                    applyReward(rule, event.getPlayer());
                }
            }
        }

        updater.sessionStart(id);
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
        UUID id = event.getPlayer().getUniqueId();
        updater.sessionEnd(id);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        String[] SubCommand = {"reload","reset","acquire","ac","recur","help"};
        String[] resetSubCommand = {"all"};
        List<String> ret= new ArrayList<String>();
        if(args.length == 1){
            for(int i = 0;i < SubCommand.length;i++){
                if(args[0].equalsIgnoreCase(SubCommand[i])) return null;
                if(args[0].length() < SubCommand[i].length()){
                    if(SubCommand[i].substring(0,args[0].length()).equalsIgnoreCase(args[0])) ret.add(SubCommand[i]);
                }
            }
        }else if(args.length == 2 && args[1].equalsIgnoreCase("reset")){
            for(int i = 0;i < resetSubCommand.length;i++){
                if(args[1].equalsIgnoreCase(resetSubCommand[i])) return null;
                if(args[1].length() < resetSubCommand[i].length()){
                    if(resetSubCommand[i].substring(0,args[1].length()).equalsIgnoreCase(args[1])) ret.add(resetSubCommand[i]);
                }
            }
        }
        if(ret.isEmpty())return null;
        return ret;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Locale.get("only-player-can-do"));
            } else if (sender.hasPermission("ptt.view")) {
                updater.updateSingle((Player) sender);
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
                    updater.resetAllStatistic();
                } else {
                    updater.resetSingleStatistic(Bukkit.getOfflinePlayer(name).getUniqueId());
                }
                sender.sendMessage(Locale.get("command-done"));
            } else {
                sender.sendMessage(Locale.get("no-permission"));
            }
            return true;
        } else if ("acquire".equalsIgnoreCase(args[0]) || "ac".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("ptt.acquire")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Locale.get("only-player-can-do"));
                    return true;
                }
                Player p = (Player) sender;

                Set<Rule> satisfiedRules = getSatisfiedRules(p.getUniqueId());
                if (satisfiedRules.size() == 0) {
                    sender.sendMessage(Locale.get("nothing-to-acquire"));
                    return true;
                }
                // feature removed: acquire particular reward
                for (Rule r : satisfiedRules) {
                    applyReward(r, p);
                    updater.markRuleAsApplied(p.getUniqueId(), r);
                }
            } else {
                sender.sendMessage(Locale.get("no-permission"));
            }
            return true;
        } else if ("recur".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("ptt.recurrence")) {
                if (args.length < 3) return false;
                String playerName = args[1];
                String ruleName = args[2];
                UUID id = getServer().getOfflinePlayer(playerName).getUniqueId();
                Rule rule = rules.get(ruleName);
                if (id != null) {
                    if (rule != null && rule.period == Rule.PeriodType.DISPOSABLE) {
                        database.setRecurrenceRule(ruleName, id);
                        database.save();
                    } else {
                        sender.sendMessage(Locale.get("invalid-rule"));
                    }
                } else {
                    sender.sendMessage(Locale.get("no-player"));
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
                    updater.updateSingle(p);
                } else {
                    //TODO: offline player update data
                }
                printStatistic(sender, p);
            } else {
                sender.sendMessage(Locale.get("no-permission"));
            }
            return true;
        }
    }

    private void printStatistic(CommandSender s, OfflinePlayer p) {
        s.sendMessage(Locale.get("statistic-for", p.getName()));
        SessionedRecord rec = updater.getFullRecord(p.getUniqueId());
        if (rec.dbRec == null) {
            s.sendMessage(Locale.get("statistic-no-record"));
        } else {
            boolean differentYear = false;
            boolean differentMonth = false;
            boolean differentWeek = false;
            boolean differentDay = false;
            if(!p.isOnline()) {
                ZonedDateTime now = ZonedDateTime.now();
                ZonedDateTime last = rec.dbRec.lastSeen;
                int diffDayOfMonth = now.getDayOfMonth() - last.getDayOfMonth();
                differentYear = last.getYear() != now.getYear();
                differentMonth = differentYear || (last.getMonth() != now.getMonth());
                differentWeek = differentMonth || Math.abs(diffDayOfMonth) >= 7 ||
                        (diffDayOfMonth * (now.getDayOfWeek().getValue() - last.getDayOfWeek().getValue())) < 0;
                differentDay = differentMonth || (diffDayOfMonth != 0);
            }
            s.sendMessage(Locale.get("statistic-day", Locale.formatTime(differentDay ? 0 : rec.dbRec.dailyTime)));
            s.sendMessage(Locale.get("statistic-week", Locale.formatTime(differentWeek ? 0 : rec.dbRec.weeklyTime)));
            s.sendMessage(Locale.get("statistic-month", Locale.formatTime(differentMonth ? 0 : rec.dbRec.monthlyTime)));
            s.sendMessage(Locale.get("statistic-total", Locale.formatTime(rec.dbRec.totalTime)));
            if (p.isOnline() && rec.getSessionTime() > 0) {
                s.sendMessage(Locale.get("statistic-session", Locale.formatTime(rec.getSessionTime())));
            }
        }
    }

    /**
     * Only DAY, WEEK, MONTH, DISPOSABLE and SESSION type will be returned here
     * LONGTIMENOSEE is checked in playerLoginEvent
     *
     * @param id uuid of the player
     * @return set of rules, not null
     */
    private Set<Rule> getSatisfiedRules(UUID id) {
        Set<Rule> ret = new HashSet<>();
        SessionedRecord rec = updater.getFullRecord(id);
        if (rec.getSessionTime() > 0) {
            ret.addAll(
                    rules.values().stream().filter(r -> r.period == Rule.PeriodType.SESSION)
                            .filter(r -> rec.getSessionTime() > r.require * 60 * 1000)
                            .filter(r -> rec.getSessionTime() < (r.require + r.timeout) * 60 * 1000 || r.timeout < 0)
                            .filter(r -> !rec.getCompletedSessionMissions().contains(r.name))
                            .filter(r -> inGroup(id, r.group))
                            .collect(Collectors.toSet())
            );
        }
        if (rec.dbRec != null) {
            ret.addAll(rules.values().stream().filter(r -> r.period == Rule.PeriodType.DAY)
                    .filter(r -> rec.dbRec.dailyTime > r.require * 60 * 1000)
                    .filter(r -> rec.dbRec.dailyTime < (r.require + r.timeout) * 60 * 1000 || r.timeout < 0)
                    .filter(r -> !rec.dbRec.completedDailyMissions.contains(r.name))
                    .filter(r -> inGroup(id, r.group))
                    .collect(Collectors.toSet()));
            ret.addAll(rules.values().stream().filter(r -> r.period == Rule.PeriodType.WEEK)
                    .filter(r -> rec.dbRec.weeklyTime > r.require * 60 * 1000)
                    .filter(r -> rec.dbRec.weeklyTime < (r.require + r.timeout) * 60 * 1000 || r.timeout < 0)
                    .filter(r -> !rec.dbRec.completedWeeklyMissions.contains(r.name))
                    .filter(r -> inGroup(id, r.group))
                    .collect(Collectors.toSet()));
            ret.addAll(rules.values().stream().filter(r -> r.period == Rule.PeriodType.MONTH)
                    .filter(r -> rec.dbRec.monthlyTime > r.require * 60 * 1000)
                    .filter(r -> rec.dbRec.monthlyTime < (r.require + r.timeout) * 60 * 1000 || r.timeout < 0)
                    .filter(r -> !rec.dbRec.completedMonthlyMissions.contains(r.name))
                    .filter(r -> inGroup(id, r.group))
                    .collect(Collectors.toSet()));
            ret.addAll(rules.values().stream().filter(r -> r.period == Rule.PeriodType.DISPOSABLE)
                    .filter(r -> rec.dbRec.totalTime > r.require * 60 * 1000)
                    .filter(r -> rec.dbRec.totalTime < (r.require + r.timeout) * 60 * 1000 || r.timeout < 0)
                    .filter(r -> !rec.dbRec.completedLifetimeMissions.contains(r.name))
                    .filter(r -> inGroup(id, r.group))
                    .collect(Collectors.toSet()));
        }
        // check all recurrence rule
        if (database.recurrenceMap.containsKey(id)) {
            for (Map.Entry<String, Long> e : database.recurrenceMap.get(id).entrySet()) {
                Rule r = rules.get(e.getKey());
                if (r == null || r.period != Rule.PeriodType.DISPOSABLE) continue;
                if (e.getValue() < r.require * 60 * 1000) continue;
                if (r.timeout > 0 && e.getValue() > (r.require + r.timeout) * 60 * 1000) continue;
                if (!inGroup(id, r.group)) continue;
                ret.add(r);
            }
        }
        return ret;
    }
}

