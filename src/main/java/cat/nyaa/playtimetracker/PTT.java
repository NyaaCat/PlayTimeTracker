package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.RecordManager.SessionedRecord;
import cat.nyaa.playtimetracker.command.CommandHandler;
import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.listener.AfkListener;
import cat.nyaa.playtimetracker.listener.PTTListener;
import cat.nyaa.playtimetracker.task.NotifyAcquireTask;
import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

public class PTT extends JavaPlugin implements Runnable {
    private static PTT instance;
    // Essential Hooks
    public IEssentials ess = null;
    private DatabaseManager database;
    private RecordManager updater;
    private Map<String, Rule> rules;
    private Map<String, Reward> rewardMap;
    public I18n i18n;
    public PTTConfiguration pttConfiguration;
    private CommandHandler commandHandler;

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

    public static boolean isAFK(UUID id) {
        return instance != null && instance.pttConfiguration.checkAfk && instance._isAFK(id);
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
        this.pttConfiguration = new PTTConfiguration(this);
        pttConfiguration.load();
        this.i18n = new I18n(this, pttConfiguration.language);
        i18n.load();
        //Locale.init(cfg.getConfigurationSection("message"));

        // Load Reward config
        rewardMap = new HashMap<>();
        pttConfiguration.rewards.forEach((k, v) -> rewardMap.put(k, new Reward(v)));
        rules = new HashMap<>();
        pttConfiguration.rules.forEach((k, v) -> rules.put(k, new Rule(k, v)));

        //command
        this.commandHandler = new CommandHandler(this, i18n);
        PluginCommand mainCommand = getCommand("playtimetracker");
        if (mainCommand != null) {
            mainCommand.setExecutor(commandHandler);
            mainCommand.setTabCompleter(commandHandler);
        } else throw new RuntimeException("Command registration failed");

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
        new PTTListener(this);
        new AfkListener(this);
        Bukkit.getScheduler().runTaskTimer(this, this, pttConfiguration.saveInterval * 20L, pttConfiguration.saveInterval * 20L);//todo remove

    }

    public void onReload() {
        onDisable();
        onEnable();
    }

    private boolean _isAFK(UUID id) {
        if (id == null) return false;
        if (pttConfiguration.useEssAfkStatus && ess != null) {
            return ess.getUser(id).isAfk();
        }
        return AfkListener.checkAfk && AfkListener.isAfk(id);
    }

    public boolean inGroup(UUID id, Set<String> group) {
        if (ess == null || group == null || id == null) return true;
        return group.contains(ess.getUser(id).getGroup());
    }

    /**
     * naively apply the rule to the player
     * database untouched
     */
    public void applyReward(Rule rule, Player p) {
        Reward reward = rewardMap.get(rule.reward);
        reward.applyTo(p);
        I18n.send(p, "info.rule-applied", rule.name);
        if (reward.getDescription() != null && reward.getDescription().length() > 0) {
            p.sendMessage(rewardMap.get(rule.reward).getDescription());
        }
        log(String.format("Reward rule %s applied to player %s", rule.name, p.getName()));
    }

    public void notifyAcquire(Player p) {
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
            I18n.send(p, "info.have-reward-redeem");
            for (Rule s : unacquired) {
                I18n.send(p, "info.have-reward-redeem-format", s.name);
            }
        }
    }

    @Override
    public void run() { // Auto-save timer
        //todo remove auto-save
        debug("Auto-save timer executing...");
        updater.updateAllOnlinePlayers();
        //todo notifyAcquireTask
        new NotifyAcquireTask(this).run();
    }



    public void printStatistic(CommandSender s, OfflinePlayer p) {
        I18n.send(s, "info/statistic.for", p.getName());
        SessionedRecord rec = updater.getFullRecord(p.getUniqueId());
        if (rec.dbRec == null) {
            I18n.send(s, "info.statistic.no-record");
        } else {
            boolean differentYear;
            boolean differentMonth = false;
            boolean differentWeek = false;
            boolean differentDay = false;
            if (!p.isOnline()) {
                ZonedDateTime now = ZonedDateTime.now();
                ZonedDateTime last = rec.dbRec.lastSeen;
                int diffDayOfMonth = now.getDayOfMonth() - last.getDayOfMonth();
                differentYear = last.getYear() != now.getYear();
                differentMonth = differentYear || (last.getMonth() != now.getMonth());
                differentWeek = differentMonth || Math.abs(diffDayOfMonth) >= 7 ||
                        (diffDayOfMonth * (now.getDayOfWeek().getValue() - last.getDayOfWeek().getValue())) < 0;
                differentDay = differentMonth || (diffDayOfMonth != 0);
            }
            I18n.send(s, "info.statistic.day", I18n.formatTime(differentDay ? 0 : rec.dbRec.dailyTime));
            I18n.send(s, "info.statistic.week", I18n.formatTime(differentWeek ? 0 : rec.dbRec.weeklyTime));
            I18n.send(s, "info.statistic.month", I18n.formatTime(differentMonth ? 0 : rec.dbRec.monthlyTime));
            I18n.send(s, "info.statistic.total", I18n.formatTime(rec.dbRec.totalTime));
            if (p.isOnline() && rec.getSessionTime() > 0) {
                I18n.send(s, "info.statistic.session", I18n.formatTime(rec.getSessionTime()));
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
    public Set<Rule> getSatisfiedRules(UUID id) {
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

    public RecordManager getUpdater() {
        return updater;
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public Map<String, Rule> getRules() {
        return rules;
    }
}

