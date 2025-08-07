package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class TestUpdateTimeRecord {

    public static final ZoneId zone = ZoneId.systemDefault();
    public static final DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
    public static final UUID playerId = UUID.randomUUID();

    @Test
    public void testUpdateTimeRecord1() {
        final var DAY_IN_MILLIS = 86400 * 1000L; // 1 day in milliseconds
        var modelTime = LocalDateTime.parse("2025-07-30T22:00:00").atZone(zone).toInstant();

        var model = new TimeTrackerDbModel(
                playerId,
                modelTime.toEpochMilli(),
                1000L, // dailyTime
                2000L, // weeklyTime
                3000L, // monthlyTime
                4000L, // totalTime
                modelTime.toEpochMilli() - DAY_IN_MILLIS // lastSeen
        );

        {
            var updateTime = LocalDateTime.parse("2025-07-30T23:00:00").atZone(zone).toInstant();
            var timeInfo = new PiecewiseTimeInfo(updateTime, zone, dayOfWeek);
            var result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, true, false);
            var expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    3600000L + model.dailyTime, // 1 hour in milliseconds
                    3600000L + model.weeklyTime, // weeklyTime remains unchanged
                    3600000L + model.monthlyTime, // monthlyTime remains unchanged
                    3600000L + model.totalTime, // totalTime updated
                    model.lastSeen
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, true, true);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    3600000L + model.dailyTime, // 1 hour in milliseconds
                    3600000L + model.weeklyTime, // weeklyTime remains unchanged
                    3600000L + model.monthlyTime, // monthlyTime remains unchanged
                    3600000L + model.totalTime, // totalTime updated
                    updateTime.toEpochMilli()
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, false, false);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    model.dailyTime, // 1 hour in milliseconds
                    model.weeklyTime, // weeklyTime remains unchanged
                    model.monthlyTime, // monthlyTime remains unchanged
                    model.totalTime, // totalTime updated
                    model.lastSeen
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, false, true);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    model.dailyTime, // 1 hour in milliseconds
                    model.weeklyTime, // weeklyTime remains unchanged
                    model.monthlyTime, // monthlyTime remains unchanged
                    model.totalTime, // totalTime updated
                    updateTime.toEpochMilli()
            );
            Assertions.assertEquals(expected, result);
        }

    }

    @Test
    public void testUpdateTimeRecord2() {
        final var DAY_IN_MILLIS = 86400 * 1000L; // 1 day in milliseconds
        var modelTime = LocalDateTime.parse("2025-07-30T22:00:00").atZone(zone).toInstant();

        var model = new TimeTrackerDbModel(
                playerId,
                modelTime.toEpochMilli(),
                1000L, // dailyTime
                2000L, // weeklyTime
                3000L, // monthlyTime
                4000L, // totalTime
                modelTime.toEpochMilli() - DAY_IN_MILLIS // lastSeen
        );

        {
            var updateTime = LocalDateTime.parse("2025-07-31T01:00:00").atZone(zone).toInstant();
            var timeInfo = new PiecewiseTimeInfo(updateTime, zone, dayOfWeek);
            var result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, true, false);
            var expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    3600000L, // 1 hour in milliseconds
                    3600000L * 3 + model.weeklyTime, // weeklyTime remains unchanged
                    3600000L * 3 + model.monthlyTime, // monthlyTime remains unchanged
                    3600000L * 3 + model.totalTime, // totalTime updated
                    model.lastSeen
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, true, true);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    3600000L, // 1 hour in milliseconds
                    3600000L * 3 + model.weeklyTime, // weeklyTime remains unchanged
                    3600000L * 3 + model.monthlyTime, // monthlyTime remains unchanged
                    3600000L * 3 + model.totalTime, // totalTime updated
                    updateTime.toEpochMilli()
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, false, false);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    0, // 1 hour in milliseconds
                    model.weeklyTime, // weeklyTime remains unchanged
                    model.monthlyTime, // monthlyTime remains unchanged
                    model.totalTime, // totalTime updated
                    model.lastSeen
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, false, true);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    0, // 1 hour in milliseconds
                    model.weeklyTime, // weeklyTime remains unchanged
                    model.monthlyTime, // monthlyTime remains unchanged
                    model.totalTime, // totalTime updated
                    updateTime.toEpochMilli()
            );
            Assertions.assertEquals(expected, result);
        }

    }


    @Test
    public void testUpdateTimeRecord3() {
        final var DAY_IN_MILLIS = 86400 * 1000L; // 1 day in milliseconds
        var modelTime = LocalDateTime.parse("2025-07-30T22:00:00").atZone(zone).toInstant();

        var model = new TimeTrackerDbModel(
                playerId,
                modelTime.toEpochMilli(),
                1000L, // dailyTime
                2000L, // weeklyTime
                3000L, // monthlyTime
                4000L, // totalTime
                modelTime.toEpochMilli() - DAY_IN_MILLIS // lastSeen
        );

        {
            var updateTime = LocalDateTime.parse("2025-08-01T01:00:00").atZone(zone).toInstant();
            var timeInfo = new PiecewiseTimeInfo(updateTime, zone, dayOfWeek);
            var result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, true, false);
            var expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    3600000L, // 1 hour in milliseconds
                    3600000L * 3 + DAY_IN_MILLIS + model.weeklyTime, // weeklyTime remains unchanged
                    3600000L, // monthlyTime remains unchanged
                    3600000L * 3 + DAY_IN_MILLIS + model.totalTime, // totalTime updated
                    model.lastSeen
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, true, true);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    3600000L, // 1 hour in milliseconds
                    3600000L * 3 + DAY_IN_MILLIS + model.weeklyTime, // weeklyTime remains unchanged
                    3600000L, // monthlyTime remains unchanged
                    3600000L * 3 + DAY_IN_MILLIS + model.totalTime, // totalTime updated
                    updateTime.toEpochMilli()
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, false, false);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    0, // 1 hour in milliseconds
                    model.weeklyTime, // weeklyTime remains unchanged
                    0, // monthlyTime remains unchanged
                    model.totalTime, // totalTime updated
                    model.lastSeen
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, false, true);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    0, // 1 hour in milliseconds
                    model.weeklyTime, // weeklyTime remains unchanged
                    0, // monthlyTime remains unchanged
                    model.totalTime, // totalTime updated
                    updateTime.toEpochMilli()
            );
            Assertions.assertEquals(expected, result);
        }

    }


    @Test
    public void testUpdateTimeRecord4() {
        final var DAY_IN_MILLIS = 86400 * 1000L; // 1 day in milliseconds
        var modelTime = LocalDateTime.parse("2025-06-29T22:00:00").atZone(zone).toInstant();

        var model = new TimeTrackerDbModel(
                playerId,
                modelTime.toEpochMilli(),
                1000L, // dailyTime
                2000L, // weeklyTime
                3000L, // monthlyTime
                4000L, // totalTime
                modelTime.toEpochMilli() - DAY_IN_MILLIS // lastSeen
        );

        {
            var updateTime = LocalDateTime.parse("2025-07-01T01:00:00").atZone(zone).toInstant();
            var timeInfo = new PiecewiseTimeInfo(updateTime, zone, dayOfWeek);
            var result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, true, false);
            var expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    3600000L, // 1 hour in milliseconds
                    3600000L + DAY_IN_MILLIS, // weeklyTime remains unchanged
                    3600000L, // monthlyTime remains unchanged
                    3600000L * 3 + DAY_IN_MILLIS + model.totalTime, // totalTime updated
                    model.lastSeen
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, true, true);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    3600000L, // 1 hour in milliseconds
                    3600000L + DAY_IN_MILLIS, // weeklyTime remains unchanged
                    3600000L, // monthlyTime remains unchanged
                    3600000L * 3 + DAY_IN_MILLIS + model.totalTime, // totalTime updated
                    updateTime.toEpochMilli()
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, false, false);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    0, // 1 hour in milliseconds
                    0, // weeklyTime remains unchanged
                    0, // monthlyTime remains unchanged
                    model.totalTime, // totalTime updated
                    model.lastSeen
            );
            Assertions.assertEquals(expected, result);

            result = cloneModel(model);
            UpdateTimeRecordTask.updateTimeTrackerDbModel(result, timeInfo, false, true);
            expected = new TimeTrackerDbModel(
                    playerId,
                    updateTime.toEpochMilli(),
                    0, // 1 hour in milliseconds
                    0, // weeklyTime remains unchanged
                    0, // monthlyTime remains unchanged
                    model.totalTime, // totalTime updated
                    updateTime.toEpochMilli()
            );
            Assertions.assertEquals(expected, result);
        }

    }

    public static TimeTrackerDbModel cloneModel(TimeTrackerDbModel model) {
        return new TimeTrackerDbModel(
                model.getPlayerUniqueId(),
                model.getLastUpdate(),
                model.getDailyTime(),
                model.getWeeklyTime(),
                model.getMonthlyTime(),
                model.getTotalTime(),
                model.getLastSeen()
        );
    }
}
