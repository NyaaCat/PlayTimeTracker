package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.condition.*;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.utils.PiecewiseTimeInfo;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class LimitedTimeTrackerModel {

    private final long MAX_MILLIS = Duration.ofNanos(Long.MAX_VALUE).toMillis();

    public final TimeTrackerDbModel model;
    public final PiecewiseTimeInfo time;

    public LimitedTimeTrackerModel(TimeTrackerDbModel model, PiecewiseTimeInfo time) {
        this.model = model;
        this.time = time;
    }

    public @Nullable Duration analyzeResolved(List<Range> result) {
        if (result.isEmpty()) {
            throw new IllegalArgumentException("result is empty");
        }
        long left = Long.MAX_VALUE;
        for(var r : result) {
            if (r.getLow() > 0 && r.getLow() < left) {
                left = r.getLow();
            }
        }
        return left > MAX_MILLIS ? null :  Duration.ofMillis(left);
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
            var limit = Range.of(current, current + source.time.getNextDayStart() - source.time.getTimestamp() - 1);
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
            var limit = Range.of(current, current + source.time.getNextWeekStart() - source.time.getTimestamp() - 1);
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
            var limit = Range.of(current, current + source.time.getNextMonthStart() - source.time.getTimestamp() - 1);
            if (target.intersect(limit)) {
                target.offset(-current);
                return List.of(target);
            } else {
                return List.of();
            }
        }
    }

    public static class TotalTime implements IParametricVariable<LimitedTimeTrackerModel> {

        private final String name;

        public TotalTime(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public long getValue(final LimitedTimeTrackerModel source) {
            return source.model.getTotalTime();
        }

        @Override
        public List<Range> resolve(Range target, final LimitedTimeTrackerModel source) {
            var current = source.model.getTotalTime();
            target.offset(-current);
            return List.of(target);
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

    public static final Map<String, IParametricVariable<LimitedTimeTrackerModel>> VARIABLES = Map.of(
        "dailyTime", new DailyTime("dailyTime"),
        "weeklyTime", new WeeklyTime("weeklyTime"),
        "monthlyTime", new MonthlyTime("monthlyTime"),
        "totalTime", new TotalTime("totalTime"),
        "lastSeen", new LastSeen("lastSeen")
    );

    public static ICondition<LimitedTimeTrackerModel> compileCondition(String condition, long precision) throws ConditionExpressionCompiler.CompileException {
        var tokenizer = new ConditionTokenizer();
        var lexer = tokenizer.parse(condition);
        var compiler = new ConditionExpressionCompiler<>(VARIABLES, precision);
        return compiler.compile(lexer);
    }
}
