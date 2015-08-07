package cat.nyaa.playtimetracker;

import org.bukkit.command.CommandSender;

import java.util.*;

public class OnlineRecord {
    private boolean parsed;
    private boolean modified;
    private String data;
    private UUID uuid;

    private long lastSeen; // timestamp millisecond
    private long dayDay; // timestamp millisecond
    private long weekDay; // timestamp millisecond
    private long monthDay; // timestamp millisecond
    private long dayTime; // millisecond
    private long weekTime; // millisecond
    private long monthTime; // millisecond
    private long totalTime; // millisecond
    private Set<String> dayComplete;
    private Set<String> weekComplete;
    private Set<String> monthComplete;
    private Set<String> disposableComplete;

    //phantom data
    private long sessionTime;
    private Set<String> sessionComplete;
    private long ltnsDay; // timestamp millisecond
    private Set<String> ltnsAvailable;

    public UUID getUuid() {
        return uuid;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public long getDayTime() {
        return dayTime;
    }

    public long getWeekTime() {
        return weekTime;
    }

    public long getMonthTime() {
        return monthTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getSessionTime() {
        return sessionTime;
    }

    public long getLtnsDay() {
        return ltnsDay;
    }

    public static OnlineRecord fromString(UUID id, String str) {
        OnlineRecord r = new OnlineRecord();
        r.uuid = id;
        r.data = str;
        r.modified = false;
        r.parsed = false;
        return r;
    }

    public void parse() {
        if (parsed) return;
        String[] tmp = data.split(" ");

        lastSeen = Long.parseLong(tmp[0]);
        dayDay = Long.parseLong(tmp[1]);
        weekDay = Long.parseLong(tmp[2]);
        monthDay = Long.parseLong(tmp[3]);
        dayTime = Long.parseLong(tmp[4]);
        weekTime = Long.parseLong(tmp[5]);
        monthTime = Long.parseLong(tmp[6]);
        totalTime = Long.parseLong(tmp[7]);
        dayComplete = deserializeSet(tmp[8]);
        weekComplete = deserializeSet(tmp[9]);
        monthComplete = deserializeSet(tmp[10]);
        disposableComplete = deserializeSet(tmp[11]);
        parsed = true;
    }

    @Override
    public String toString() {
        if (modified) {
            String s1 = String.format("%d %d %d %d %d %d %d %d", lastSeen, dayDay, weekDay, monthDay,
                    dayTime, weekTime, monthTime, totalTime);
            s1 = String.format("%s %s %s %s %s", s1,
                    serializeSet(dayComplete),
                    serializeSet(weekComplete),
                    serializeSet(monthComplete),
                    serializeSet(disposableComplete));
            data = s1;
            modified = false;
        }
        return data;
    }

    private String serializeSet(Set<String> set) {
        if (set.size() == 0) return "{}";
        String ret = "";
        for (String s : set) {
            ret += "," + s;
        }
        return "{" + ret.substring(1) + "}";
    }

    private HashSet<String> deserializeSet(String str) {
        if ("{}".equals(str)) return new HashSet<>();
        HashSet<String> r = new HashSet<>();
        r.addAll(Arrays.asList(str.substring(1, str.length() - 1).split(",")));
        return r;
    }

    public OnlineRecord() {
        reset();
    }

    public static OnlineRecord createFor(UUID uuid) {
        OnlineRecord record = new OnlineRecord();
        record.uuid = uuid;
        return record;
    }

    public void update(boolean accumulate) {
        parse();
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        long nowTime = date.getTime();
        long delta = nowTime - lastSeen;
        lastSeen = nowTime;
        if (accumulate) totalTime += delta;

        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (dayDay < cal.getTimeInMillis()) {
            dayDay = cal.getTimeInMillis();
            dayTime = 0;
            dayComplete = new HashSet<>();
        } else {
            if (accumulate) dayTime += delta;
        }
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        if (weekDay < cal.getTimeInMillis()) {
            weekDay = cal.getTimeInMillis();
            weekTime = 0;
            weekComplete = new HashSet<>();
        } else {
            if (accumulate) weekTime += delta;
        }
        cal.set(Calendar.DAY_OF_MONTH, 1);
        if (monthDay < cal.getTimeInMillis()) {
            monthDay = cal.getTimeInMillis();
            monthTime = 0;
            monthComplete = new HashSet<>();
        } else {
            if (accumulate) monthTime += delta;
        }
        if (accumulate) sessionTime += delta;

        modified = true;
    }

    public Boolean isCompleted(Rule.PeriodType type, String rule) {
        parse();
        switch (type) {
            case DAY:
                return dayComplete.contains(rule);
            case WEEK:
                return weekComplete.contains(rule);
            case MONTH:
                return monthComplete.contains(rule);
            case DISPOSABLE:
                return disposableComplete.contains(rule);
            case SESSION:
                return sessionComplete.contains(rule);
            case LONGTIMENOSEE:
                return !ltnsAvailable.contains(rule);
        }
        return false;
    }

    public void setCompleted(Rule.PeriodType type, String rule) {
        parse();
        if (isCompleted(type, rule)) return;
        switch (type) {
            case DAY: {
                dayComplete.add(rule);
                break;
            }
            case WEEK: {
                weekComplete.add(rule);
                break;
            }
            case MONTH: {
                monthComplete.add(rule);
                break;
            }
            case DISPOSABLE: {
                disposableComplete.add(rule);
                break;
            }
            case SESSION: {
                sessionComplete.add(rule);
                break;
            }
            case LONGTIMENOSEE: {
                ltnsAvailable.remove(rule);
                break;
            }
        }
        modified = true;
    }

    public void reset() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        lastSeen = date.getTime();

        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        dayDay = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        weekDay = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        monthDay = cal.getTimeInMillis();

        dayTime = 0;
        weekTime = 0;
        monthTime = 0;
        totalTime = 0;
        disposableComplete = new HashSet<>();
        dayComplete = new HashSet<>();
        weekComplete = new HashSet<>();
        monthComplete = new HashSet<>();

        sessionComplete = new HashSet<>();
        sessionTime = 0;

        modified = true;
        parsed = true;
    }

    public void clearSession() {
        sessionTime = 0;
        sessionComplete = new HashSet<>();
        ltnsAvailable = new HashSet<>();
    }

    public void addLtnsRule(String ruleName) {
        ltnsAvailable.add(ruleName);
    }

    public void printStatistic(CommandSender s, boolean printSession) {
        parse();
        s.sendMessage(Locale.get("statistic-day", Locale.formatTime(getDayTime())));
        s.sendMessage(Locale.get("statistic-week", Locale.formatTime(getWeekTime())));
        s.sendMessage(Locale.get("statistic-month", Locale.formatTime(getMonthTime())));
        s.sendMessage(Locale.get("statistic-total", Locale.formatTime(getTotalTime())));
        if (printSession) {
            s.sendMessage(Locale.get("statistic-session", Locale.formatTime(sessionTime)));
        }
    }
}
