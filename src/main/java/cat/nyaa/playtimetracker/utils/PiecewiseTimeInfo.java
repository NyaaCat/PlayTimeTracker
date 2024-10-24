package cat.nyaa.playtimetracker.utils;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;


public class PiecewiseTimeInfo {
    public final ZoneId zone;
    public final DayOfWeek dayOfWeek;
    private Instant time;
    private Instant cachedSameDayStart;
    private Instant cachedNextDayStart;
    private Instant cachedSameWeekStart;
    private Instant cachedNextWeekStart;
    private Instant cachedSameMonthStart;
    private Instant cachedNextMonthStart;
    private Duration cachedDurationFromSameDayStart;
    private Duration cachedDurationToNextDayStart;
    private Duration cachedDurationFromSameWeekStart;
    private Duration cachedDurationToNextWeekStart;
    private Duration cachedDurationFromSameMonthStart;
    private Duration cachedDurationToNextMonthStart;

    public PiecewiseTimeInfo(Instant time, ZoneId zone, DayOfWeek dayOfWeek) {
        this.zone = zone;
        this.dayOfWeek = dayOfWeek;
        this.cachedSameDayStart = null;
        this.cachedNextDayStart = null;
        this.cachedSameWeekStart = null;
        this.cachedNextWeekStart = null;
        this.cachedSameMonthStart = null;
        this.cachedNextMonthStart = null;
        this.cachedDurationFromSameDayStart = null;
        this.cachedDurationToNextDayStart = null;
        this.cachedDurationFromSameWeekStart = null;
        this.cachedDurationToNextWeekStart = null;
        this.cachedDurationFromSameMonthStart = null;
        this.cachedDurationToNextMonthStart = null;
        this.updateTime(time);
    }

    public Instant updateTime(final Instant time) {
        this.time = time;
        ZonedDateTime dateTime = null;
        if (this.cachedNextDayStart == null || time.compareTo(this.cachedNextDayStart) >= 0) {
            dateTime = ZonedDateTime.ofInstant(time, this.zone);
            var today = dateTime.truncatedTo(ChronoUnit.DAYS);
            this.cachedSameDayStart = today.toInstant();
            var tomorrow = today.plusDays(1);
            this.cachedNextDayStart = tomorrow.toInstant();
        }
        if (this.cachedNextWeekStart == null || time.compareTo(this.cachedNextWeekStart) >= 0) {
            if (dateTime == null) {
                dateTime = ZonedDateTime.ofInstant(time, this.zone);
            }
            var thisWeek = dateTime.with(TemporalAdjusters.previousOrSame(this.dayOfWeek))
                    .truncatedTo(ChronoUnit.DAYS);
            this.cachedSameWeekStart = thisWeek.toInstant();
            var nextWeek = thisWeek.plusWeeks(1);
            this.cachedNextWeekStart = nextWeek.toInstant();
        }
        if (this.cachedNextMonthStart == null || time.compareTo(this.cachedNextMonthStart) >= 0) {
            if (dateTime == null) {
                dateTime = ZonedDateTime.ofInstant(time, this.zone);
            }
            var thisMonth = dateTime.with(TemporalAdjusters.firstDayOfMonth())
                    .truncatedTo(ChronoUnit.DAYS);
            this.cachedSameMonthStart = thisMonth.toInstant();
            var nextMonth = thisMonth.plusMonths(1);
            this.cachedNextMonthStart = nextMonth.toInstant();
        }
        this.cachedDurationFromSameDayStart = null;
        this.cachedDurationToNextDayStart = null;
        this.cachedDurationFromSameWeekStart = null;
        this.cachedDurationToNextWeekStart = null;
        this.cachedDurationFromSameMonthStart = null;
        this.cachedDurationToNextMonthStart = null;
        return this.time;
    }

    public Instant getTime() {
        return this.time;
    }

    public Instant getSameDayStart() {
        return this.cachedSameDayStart;
    }

    public Duration durationFromSameDayStart() {
        if (this.cachedDurationFromSameDayStart == null) {
            this.cachedDurationFromSameDayStart = Duration.between(this.cachedSameDayStart, this.time);
        }
        return this.cachedDurationFromSameDayStart;
    }

    public Instant getNextDayStart() {
        return this.cachedNextDayStart;
    }

    public Duration durationToNextDayStart() {
        if (this.cachedDurationToNextDayStart == null) {
            this.cachedDurationToNextDayStart = Duration.between(this.time, this.cachedNextDayStart);
        }
        return this.cachedDurationToNextDayStart;
    }

    public Instant getSameWeekStart() {
        return this.cachedSameWeekStart;
    }

    public Duration durationFromSameWeekStart() {
        if (this.cachedDurationFromSameWeekStart == null) {
            this.cachedDurationFromSameWeekStart = Duration.between(this.cachedSameWeekStart, this.time);
        }
        return this.cachedDurationFromSameWeekStart;
    }

    public Instant getNextWeekStart() {
        return this.cachedNextWeekStart;
    }

    public Duration durationToNextWeekStart() {
        if (this.cachedDurationToNextWeekStart == null) {
            this.cachedDurationToNextWeekStart = Duration.between(this.time, this.cachedNextWeekStart);
        }
        return this.cachedDurationToNextWeekStart;
    }

    public Instant getSameMonthStart() {
        return this.cachedSameMonthStart;
    }

    public Duration durationFromSameMonthStart() {
        if (this.cachedDurationFromSameMonthStart == null) {
            this.cachedDurationFromSameMonthStart = Duration.between(this.cachedSameMonthStart, this.time);
        }
        return this.cachedDurationFromSameMonthStart;
    }

    public Instant getNextMonthStart() {
        return this.cachedNextMonthStart;
    }

    public Duration durationToNextMonthStart() {
        if (this.cachedDurationToNextMonthStart == null) {
            this.cachedDurationToNextMonthStart = Duration.between(this.time, this.cachedNextMonthStart);
        }
        return this.cachedDurationToNextMonthStart;
    }
}
