package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.condition.ComparisonOperator;
import cat.nyaa.playtimetracker.condition.Condition;
import cat.nyaa.playtimetracker.condition.ICondition;
import cat.nyaa.playtimetracker.condition.Range;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.UUID;

public class TestLimitedTimeTrackerModel {

    public static ZoneId zoneId = ZoneId.systemDefault();
    public static DayOfWeek dayOfWeek = DayOfWeek.MONDAY;

    public static UUID playerId = UUID.randomUUID();

    private final long TIME_DAY_MILLIS = 24 * 60 * 60 * 1000; // 1 day in milliseconds
    private final long TIME_WEEK_MILLIS = 7 * TIME_DAY_MILLIS; // 1 week in milliseconds

    @Test
    public void testDailyTime() {
        var variable = new LimitedTimeTrackerModel.DailyTime("dailyTime");
        var condition = new Condition<>(ComparisonOperator.GREATER_THAN_OR_EQUAL, variable, 30 * 60 * 1000, 1L);
        testInner(condition, "2025-07-19T22:00:00",  15 * 60 * 1000, Range.of(15 * 60 * 1000, 2 * 60 * 60 * 1000 - 1));
        testInner(condition,"2025-07-19T23:45:00",  15 * 60 * 1000, null);
    }

    @Test
    public void testWeeklyTime() {
        var variable = new LimitedTimeTrackerModel.WeeklyTime("weeklyTime");
        var condition = new Condition<>(ComparisonOperator.GREATER_THAN_OR_EQUAL, variable, 2 * 60 * 60 * 1000, 1L);
        testInner(condition,"2025-07-19T22:00:00",  1 * 60 * 60 * 1000, Range.of(1 * 60 * 60 * 1000, 26 * 60 * 60 * 1000 - 1));
        testInner(condition,"2025-07-20T23:45:00", 1 * 60 * 60 * 1000, null);
    }

    @Test
    public void testMonthlyTime() {
        var variable = new LimitedTimeTrackerModel.MonthlyTime("monthlyTime");
        var condition = new Condition<>(ComparisonOperator.GREATER_THAN_OR_EQUAL, variable, 5 * 60 * 60 * 1000, 1L);
        testInner(condition,"2025-07-19T22:00:00",  2 * 60 * 60 * 1000, Range.of(3 * 60 * 60 * 1000, (2 + 12 * 24) * 60 * 60 * 1000 - 1));
        testInner(condition,"2025-07-30T23:45:00", 2 * 60 * 60 * 1000, Range.of(3 * 60 * 60 * 1000, (24 * 60 + 15) * 60 * 1000 - 1));
        testInner(condition,"2025-07-31T23:45:00", 2 * 60 * 60 * 1000, null);
    }

    @Test
    public void testTotalTime() {
        var variable = new LimitedTimeTrackerModel.TotalTime("totalTime");
        var condition = new Condition<>(ComparisonOperator.GREATER_THAN_OR_EQUAL, variable, 10 * 60 * 1000, 1L);
        testInner(condition,"2025-07-19T22:00:00",  5 * 60 * 1000, Range.upper(5 * 60 * 1000));
        testInner(condition,"2025-07-30T23:45:00", 5 * 60 * 1000, Range.upper(5 * 60 * 1000));
        testInner(condition,"2025-07-31T23:45:00", 5 * 60 * 1000, Range.upper(5 * 60 * 1000));
    }

    public void testInner(ICondition<LimitedTimeTrackerModel> condition, String time0, long accumulate, @Nullable Range expected) {


        var time = LocalDateTime.parse(time0)
                .atZone(zoneId)
                .toInstant();
        var timeInfo = new PiecewiseTimeInfo(time, zoneId, dayOfWeek);
        var model = new TimeTrackerDbModel(
                playerId,
                time.toEpochMilli(),
                Math.min(accumulate, TIME_DAY_MILLIS / 2),
                Math.min(accumulate, TIME_WEEK_MILLIS / 2),
                accumulate,
                accumulate,
                time.toEpochMilli()
        );
        var x = new LimitedTimeTrackerModel(model, timeInfo);

        var result = condition.test(x);
        Assertions.assertFalse(result);

        var wait = condition.resolve(x);
        if (expected == null) {
            Assertions.assertTrue(wait.isEmpty(), "Expected no wait time, but got: " + wait);
        } else {
            Assertions.assertEquals(1, wait.size(), "Expected one wait time, but got: " + wait);
            var actual = wait.getFirst();
            Assertions.assertEquals(expected, actual);
        }
    }
}
