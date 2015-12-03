package cat.nyaa.playtimetracker;

import com.google.common.io.LineReader;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RecordMgr {
    private final Main plugin;
    private Map<String, OnlineRecord> recordMap;

    public RecordMgr(Main plugin) {
        this.plugin = plugin;
        load();
    }

    private OnlineRecord getRecord(String playerName) {
        if (recordMap.containsKey(playerName)) return recordMap.get(playerName);
        OnlineRecord r = OnlineRecord.createFor(playerName);
        recordMap.put(playerName, r);
        return r;
    }

    public void load() {
        recordMap = new HashMap<>();
        FileReader fr = null;
        LineReader lr;
        try {
            File flatFile = new File(plugin.getDataFolder(), "data.txt");
            flatFile.createNewFile();
            fr = new FileReader(flatFile);
            lr = new LineReader(fr);
            String line;
            while ((line = lr.readLine()) != null) {
                String[] tmp = line.split(" ", 2);
                if (tmp.length != 2) continue;
                String a = null;
                try {
                    a = tmp[0];
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Illegal data line: " + line);
                    continue;
                }
                recordMap.put(a, OnlineRecord.fromString(a, tmp[1]));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fr != null) fr.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void save() {
        FileWriter fw = null;
        try {
            File flatFile = new File(plugin.getDataFolder(), "data.txt");
            flatFile.createNewFile();
            fw = new FileWriter(flatFile);

            for (OnlineRecord r : recordMap.values()) {
                fw.write(r.getPlayerName().toString() + " " + r.toString() + '\n');
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fw != null) fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateSingle(p);
        }
    }

    public void updateOnline(boolean accumulate) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateSingle(p, accumulate);
        }
    }

    public void updateSingle(Player p) {
        updateSingle(p, !plugin.isAFK(p.getName()));
    }

    public void updateSingle(Player p, boolean accumulate) {
        OnlineRecord r = getRecord(p.getName());
        if (r != null) r.update(accumulate);
    }

    public Set<Rule> getSatisfiedRules(String id) {
        OnlineRecord onlineRecord = getRecord(id);
        Set<Rule> ret = new HashSet<>();
        if (onlineRecord == null) return ret;
        for (Rule rule : plugin.getRules().values()) {
            if (!plugin.inGroup(id, rule.group)) continue;
            String ruleName = rule.name;
            switch (rule.period) {
                case LONGTIMENOSEE: {
                    if (!onlineRecord.isCompleted(Rule.PeriodType.LONGTIMENOSEE, ruleName)) {
                        long currentTime = new Date().getTime();
                        if (rule.timeout != -1 && currentTime > onlineRecord.getLtnsDay() + rule.timeout * 60 * 1000) {
                            onlineRecord.setCompleted(Rule.PeriodType.LONGTIMENOSEE, ruleName);
                        } else {
                            ret.add(rule);
                        }
                    }
                }
                default: {
                    long L = rule.require * 1000 * 60, H = L + rule.timeout * 1000 * 60;
                    long onTime = 0;
                    switch (rule.period) {
                        case DAY: {
                            onTime = onlineRecord.getDayTime();
                            break;
                        }
                        case WEEK: {
                            onTime = onlineRecord.getWeekTime();
                            break;
                        }
                        case MONTH: {
                            onTime = onlineRecord.getMonthTime();
                            break;
                        }
                        case SESSION: {
                            onTime = onlineRecord.getSessionTime();
                            break;
                        }
                        case DISPOSABLE: {
                            onTime = onlineRecord.getTotalTime();
                            break;
                        }
                    }
                    if (onTime >= L && (rule.timeout == -1 || onTime <= H) && !onlineRecord.isCompleted(rule.period, rule.name)) {
                        ret.add(rule);
                    }
                }
            }
        }
        return ret;
    }

    public void setRuleAcquired(String id, Rule rule) {
        getRecord(id).setCompleted(rule.period, rule.name);
    }

    public void resetAll() {
        for (OnlineRecord r : recordMap.values()) {
            r.reset();
        }
    }

    public void reset(String id) {
        getRecord(id).reset();
    }

    public void sessionStart(String id, Collection<Rule> rules) {
        OnlineRecord r = getRecord(id);
        r.clearSession();
        long nowTime = new Date().getTime();
        for (Rule rule : rules) {
            if (rule.period == Rule.PeriodType.LONGTIMENOSEE) {
                String ruleName = rule.name;
                if (nowTime - r.getLastSeen() >= rule.require * 24 * 60 * 60 * 1000L) {
                    r.addLtnsRule(ruleName);
                }
            }
        }
        r.setLtnsDay(nowTime);
    }

    public void sessionEnd(String id) {
        getRecord(id).clearSession();
    }

    public void printStatistic(CommandSender target, OfflinePlayer player) {
        if (recordMap.containsKey(player.getName())) {
            recordMap.get(player.getName()).printStatistic(target, player.isOnline());
        } else {
            target.sendMessage(Locale.get("statistic-no-record"));
        }
    }
}
