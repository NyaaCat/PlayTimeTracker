package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.condition.IParametricVariable;
import cat.nyaa.playtimetracker.condition.Range;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;

import java.util.List;

public class LimitedTimeTrackerModel {

    public final TimeTrackerDbModel model;
    private final PiecewiseTimeInfo time;

    public LimitedTimeTrackerModel(TimeTrackerDbModel model, PiecewiseTimeInfo time) {
        this.model = model;
        this.time = time;

    }


    public static final class DailyTime implements IParametricVariable<LimitedTimeTrackerModel> {

        private final String name;

        public DailyTime(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public long getValue(final LimitedTimeTrackerModel source) {
            return source.model.getDailyTime();
        }

        @Override
        public List<Range> resolve(Range target, final LimitedTimeTrackerModel source) {
            var current = source.model.getDailyTime();
            var limit = Range.of(current, source.time.getNextDayStart() - 1);
            if (target.intersect(limit)) {
                target.offset(-current);
                return List.of(target);
            } else {
                return List.of();
            }
        }
    }

    public static class WeeklyTime implements IParametricVariable<LimitedTimeTrackerModel> {

        private final String name;

        public WeeklyTime(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public long getValue(final LimitedTimeTrackerModel source) {
            return source.model.getWeeklyTime();
        }

        @Override
        public List<Range> resolve(Range target, final LimitedTimeTrackerModel source) {
            var current = source.model.getWeeklyTime();
            var limit = Range.of(current, source.time.getNextWeekStart() - 1);
            if (target.intersect(limit)) {
                target.offset(-current);
                return List.of(target);
            } else {
                return List.of();
            }
        }
    }

    public static class MonthlyTime implements IParametricVariable<LimitedTimeTrackerModel> {

        private final String name;

        public MonthlyTime(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public long getValue(final LimitedTimeTrackerModel source) {
            return source.model.getMonthlyTime();
        }

        @Override
        public List<Range> resolve(Range target, final LimitedTimeTrackerModel source) {
            var current = source.model.getMonthlyTime();
            var limit = Range.of(current, source.time.getNextMonthStart() - 1);
            if (target.intersect(limit)) {
                target.offset(-current);
                return List.of(target);
            } else {
                return List.of();
            }
        }
    }

    public static class LastSeen implements IParametricVariable<LimitedTimeTrackerModel> {

        private final String name;

        public LastSeen(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public long getValue(final LimitedTimeTrackerModel source) {
            return source.time.getTimestamp() - source.model.getLastSeen();
        }

        @Override
        public List<Range> resolve(Range target, final LimitedTimeTrackerModel source) {
            var current = source.time.getTimestamp() - source.model.getLastSeen();
            var limit = Range.upper(current);
            if (target.intersect(limit)) {
                target.offset(-current);
                return List.of(target);
            } else {
                return List.of();
            }
        }
    }
}
