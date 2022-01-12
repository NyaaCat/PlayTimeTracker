package cat.nyaa.playtimetracker.Utils;

import cat.nyaa.playtimetracker.PlayTimeTracker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

public class TimeUtils {
    public static long getUnixTimeStampNow() {
        return System.currentTimeMillis();
    }

    public static LocalDate getLocalDate() {
        return LocalDate.now(getZoneId());
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
