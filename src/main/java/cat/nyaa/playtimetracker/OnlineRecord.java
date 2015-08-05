package cat.nyaa.playtimetracker;

import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "playtimetracker")
public class OnlineRecord {
    @Id
    @NotEmpty
    private String uuid;
    @NotNull
    private long lastSeen; // timestamp millisecond
    @NotNull
    private long dayDay; // timestamp millisecond
    @NotNull
    private long weekDay; // timestamp millisecond
    @NotNull
    private long monthDay; // timestamp millisecond
    @NotNull
    private long dayTime; // millisecond
    @NotNull
    private long weekTime; // millisecond
    @NotNull
    private long monthTime; // millisecond
    @NotNull
    private long totalTime; // millisecond
    @NotNull
    private String disposableComplete; // ',' comma split string
    @NotNull
    private String dayComplete; // ',' comma split string
    @NotNull
    private String weekComplete; // ',' comma split string
    @NotNull
    private String monthComplete; // ',' comma split string

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getDayDay() {
        return dayDay;
    }

    public void setDayDay(long dayDay) {
        this.dayDay = dayDay;
    }

    public long getWeekDay() {
        return weekDay;
    }

    public void setWeekDay(long weekDay) {
        this.weekDay = weekDay;
    }

    public long getMonthDay() {
        return monthDay;
    }

    public void setMonthDay(long monthDay) {
        this.monthDay = monthDay;
    }

    public long getDayTime() {
        return dayTime;
    }

    public void setDayTime(long dayTime) {
        this.dayTime = dayTime;
    }

    public long getWeekTime() {
        return weekTime;
    }

    public void setWeekTime(long weekTime) {
        this.weekTime = weekTime;
    }

    public long getMonthTime() {
        return monthTime;
    }

    public void setMonthTime(long monthTime) {
        this.monthTime = monthTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public String getDisposableComplete() {
        return disposableComplete;
    }

    public void setDisposableComplete(String disposableComplete) {
        this.disposableComplete = disposableComplete;
    }

    public static OnlineRecord createFor(UUID uuid) {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        OnlineRecord record = new OnlineRecord();
        record.uuid = uuid.toString();
        record.lastSeen = date.getTime();

        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        record.dayDay = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        record.weekDay = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH,1);
        record.monthDay = cal.getTimeInMillis();

        record.dayTime=0;
        record.weekTime=0;
        record.monthTime=0;
        record.totalTime=0;
        record.disposableComplete = "";
        record.dayComplete = "";
        record.weekComplete = "";
        record.monthComplete = "";

        return record;
    }

    public void update(boolean accumulate) {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        long nowTime = date.getTime();
        long delta = nowTime - lastSeen;
        if (accumulate) totalTime += delta;

        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (dayDay < cal.getTimeInMillis()) {
            dayDay = cal.getTimeInMillis();
            dayTime = 0;
            dayComplete = "";
        } else {
            if (accumulate) dayTime += delta;
        }
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        if (weekDay < cal.getTimeInMillis()) {
            weekDay = cal.getTimeInMillis();
            weekTime = 0;
            weekComplete = "";
        }else{
            if (accumulate) weekTime += delta;
        }
        cal.set(Calendar.DAY_OF_MONTH,1);
        if (monthDay < cal.getTimeInMillis()){
            monthDay = cal.getTimeInMillis();
            monthTime=0;
            monthComplete = "";
        }else{
            if (accumulate) monthTime+=delta;
        }
    }

    public Boolean isCompleted(Rule.PeriodType type, String rule){
        String a=null;
        switch (type) {
            case DAY: {a=dayComplete;break;}
            case WEEK: {a=weekComplete;break;}
            case MONTH: {a=monthComplete;break;}
            case DISPOSABLE: {a=disposableComplete;break;}
        }
        if (a==null)return false;
        return a.equals(rule) || a.startsWith(rule+",") || a.endsWith(","+rule);
    }

    public void setCompleted(Rule.PeriodType type, String rule){
        if (type== Rule.PeriodType.SESSION||isCompleted(type, rule)) return;
        String a;
        switch (type) {
            case DAY: {a=dayComplete;break;}
            case WEEK: {a=weekComplete;break;}
            case MONTH: {a=monthComplete;break;}
            case DISPOSABLE: {a=disposableComplete;break;}
            default: a=null;
        }
        if (a==null) a="";
        if ("".equals(a)){
            a=rule;
        }else{
            a+=","+rule;
        }
        switch (type) {
            case DAY: {dayComplete=a;break;}
            case WEEK: {weekComplete=a;break;}
            case MONTH: {monthComplete=a;break;}
            case DISPOSABLE: {disposableComplete=a;break;}
        }
    }
}
