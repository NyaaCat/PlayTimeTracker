package cat.nyaa.playtimetracker.Utils;

import cat.nyaa.playtimetracker.PlayTimeTracker;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {
    public static SimpleDateFormat simpleDateFormat;

    public static long getUnixTimeStampNow() {
        return System.currentTimeMillis();
    }

    public static LocalDate getLocalDate() {
        return LocalDate.now(getZoneId());
    }

    public static String dateFormat(long timestamp) {
        if (simpleDateFormat == null) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd DD HH:mm:ss.SSS (z)");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone(getZoneId()));
        }
        return simpleDateFormat.format(new Date(timestamp));
    }

    public static String timeFormat(long time) {
        int d = Math.floorMod(time, (24 * 3600 * 1000));
        time -= 24L * 3600L * 1000L * d;
        int h = Math.floorMod(time, 3600 * 1000);
        time -= 3600L * 1000L * h;
        int m = Math.floorMod(time, 60 * 1000);
        time -= 60L * 1000L * m;
        int s = Math.floorMod(time, 1000);
        time -= 1000L * s;
        int ms = (int) time;
        String result = "";
        if (d > 0) result += d + "D";
        if (h > 0) result += h + "H";
        if (m > 0) result += m + "M";
        if (s > 0) result += s + "S";
        if (ms > 0) result += ms + "ms";
        return result;
    }

    public static ZonedDateTime timeStamp2ZonedDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(getZoneId());
    }

    public static Calendar getCalender() {
        return Calendar.getInstance(TimeZone.getTimeZone(getZoneId()));
    }

    private static ZoneId getZoneId() {
        ZoneId zoneId = null;
        if (PlayTimeTracker.getInstance() != null) {
            zoneId = PlayTimeTracker.getInstance().getTimezone();
        }
        if (zoneId == null) zoneId = ZoneId.systemDefault();
        return zoneId;
    }
}
