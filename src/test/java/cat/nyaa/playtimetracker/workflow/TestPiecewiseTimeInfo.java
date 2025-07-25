package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TestPiecewiseTimeInfo {

    public static ZoneId zoneId = ZoneId.systemDefault();
    public static DayOfWeek dayOfWeek = DayOfWeek.MONDAY;

    @Test
    public void testPiecewiseTimeInfo() {
        var inputList = new String[]{
                "2025-07-19T00:00:00",
                "2025-07-19T02:00:00",
                "2025-07-19T04:00:00",
                "2025-07-19T06:00:00",
                "2025-07-19T08:00:00",
                "2025-07-19T10:00:00",
                "2025-07-19T12:00:00",
                "2025-07-19T14:00:00",
                "2025-07-19T16:00:00",
                "2025-07-19T18:00:00",
                "2025-07-19T20:00:00",
                "2025-07-19T22:00:00",
                "2025-07-19T23:59:50",
        };
        for (var s : inputList) {
            testInner(
                    s,
                    "2025-07-19T00:00:00",
                    "2025-07-20T00:00:00",
                    "2025-07-14T00:00:00",
                    "2025-07-21T00:00:00",
                    "2025-07-01T00:00:00",
                    "2025-08-01T00:00:00"
            );
        }
    }

    public void testInner(String time, String day, String nextDay, String week, String nextWeek, String month, String nextMonth) {
        var time0 = LocalDateTime.parse(time).atZone(zoneId).toInstant();
        var day0 = LocalDateTime.parse(day).atZone(zoneId).toEpochSecond() * 1000;
        var nextDay0 = LocalDateTime.parse(nextDay).atZone(zoneId).toEpochSecond() * 1000;
        var week0 = LocalDateTime.parse(week).atZone(zoneId).toEpochSecond() * 1000;
        var nextWeek0 = LocalDateTime.parse(nextWeek).atZone(zoneId).toEpochSecond() * 1000;
        var month0 = LocalDateTime.parse(month).atZone(zoneId).toEpochSecond() * 1000;
        var nextMonth0 = LocalDateTime.parse(nextMonth).atZone(zoneId).toEpochSecond() * 1000;

        var model = new PiecewiseTimeInfo(time0, zoneId, dayOfWeek);
        Assertions.assertEquals(model.getTimestamp(), time0.toEpochMilli());
        Assertions.assertEquals(model.getSameDayStart(), day0);
        Assertions.assertEquals(model.getNextDayStart(), nextDay0);
        Assertions.assertEquals(model.getSameWeekStart(), week0);
        Assertions.assertEquals(model.getNextWeekStart(), nextWeek0);
        Assertions.assertEquals(model.getSameMonthStart(), month0);
        Assertions.assertEquals(model.getNextMonthStart(), nextMonth0);
    }
}
