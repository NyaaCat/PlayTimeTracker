package cat.nyaa.playtimetracker.utils;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;


public class PiecewiseTimeInfo {
    public final ZoneId zone;
    public final DayOfWeek dayOfWeek;

    private long time;
    private long cachedSameDayStart;
    private long cachedNextDayStart;
    private long cachedSameWeekStart;
    private long cachedNextWeekStart;
    private long cachedSameMonthStart;
    private long cachedNextMonthStart;

    public PiecewiseTimeInfo(Instant time, ZoneId zone, DayOfWeek dayOfWeek) {
        this.zone = zone;
        this.dayOfWeek = dayOfWeek;
        this.cachedNextDayStart = 0;
        this.cachedNextWeekStart = 0;
        this.cachedNextMonthStart = 0;
        this.updateTime(time);
    }

    public void updateTime(final Instant time) {
        this.time = time.toEpochMilli();
        ZonedDateTime dateTime = null;
        if (this.time >= this.cachedNextDayStart) {
            dateTime = ZonedDateTime.ofInstant(time, this.zone);
            var today = dateTime.truncatedTo(ChronoUnit.DAYS);
            this.cachedSameDayStart = today.toEpochSecond() * 1000;
            var tomorrow = today.plusDays(1);
            this.cachedNextDayStart = tomorrow.toEpochSecond() * 1000;
        }
        if (this.time >= this.cachedNextWeekStart) {
            if (dateTime == null) {
                dateTime = ZonedDateTime.ofInstant(time, this.zone);
            }
            var thisWeek = dateTime.with(TemporalAdjusters.previousOrSame(this.dayOfWeek))
                    .truncatedTo(ChronoUnit.DAYS);
            this.cachedSameWeekStart = thisWeek.toEpochSecond() * 1000;
            var nextWeek = thisWeek.plusWeeks(1);
            this.cachedNextWeekStart = nextWeek.toEpochSecond() * 1000;
        }
        if (this.time >= this.cachedNextMonthStart) {
            if (dateTime == null) {
                dateTime = ZonedDateTime.ofInstant(time, this.zone);
            }
            var thisMonth = dateTime.with(TemporalAdjusters.firstDayOfMonth())
                    .truncatedTo(ChronoUnit.DAYS);
            this.cachedSameMonthStart = thisMonth.toEpochSecond() * 1000;
            var nextMonth = thisMonth.plusMonths(1);
            this.cachedNextMonthStart = nextMonth.toEpochSecond() * 1000;
        }
    }

    public long getTimestamp() {
        return this.time;
    }

    public long getSameDayStart() {
        return this.cachedSameDayStart;
    }

    public long getNextDayStart() {
        return this.cachedNextDayStart;
    }

    public long getSameWeekStart() {
        return this.cachedSameWeekStart;
    }

    public long getNextWeekStart() {
        return this.cachedNextWeekStart;
    }

    public long getSameMonthStart() {
        return this.cachedSameMonthStart;
    }

    public long getNextMonthStart() {
        return this.cachedNextMonthStart;
    }
}
