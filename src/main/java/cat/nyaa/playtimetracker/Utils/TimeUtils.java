package cat.nyaa.playtimetracker.Utils;

import cat.nyaa.playtimetracker.I18n;
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
    private static SimpleDateFormat simpleDateFormat;

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
        long h = time / (3600L * 1000L);
        time -= 3600L * 1000L * h;
        long m = time / (60 * 1000);
        time -= 60L * 1000L * m;
        long s = time / 1000L;
        time -= 1000L * s;
        String result = "";
        if (h > 0) result += h + I18n.format("time.format.h");
        if (m > 0) result += m + I18n.format("time.format.m");
        if (s > 0) result += s + I18n.format("time.format.s");
        if (time > 0) result += time + I18n.format("time.format.ms");
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
