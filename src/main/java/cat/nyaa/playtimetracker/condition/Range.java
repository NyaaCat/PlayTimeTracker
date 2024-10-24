package cat.nyaa.playtimetracker.condition;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Range {

    // Long.MIN_VALUE as negative infinity
    private long low;

    // Long.MAX_VALUE as positive infinity
    private long high;

    public Range(long low, long high) {
        this.low = low;
        this.high = high;
    }

    public long getLow() {
        return low;
    }

    public long getHigh() {
        return high;
    }

    public void offset(long offset) {
        if(this.low != Long.MIN_VALUE) {
            this.low += offset;
        }
        if(this.high != Long.MAX_VALUE) {
            this.high += offset;
        }
    }

    public void offset(long low, long high) {
        if(this.low != Long.MIN_VALUE) {
            this.low += low;
        }
        if(this.high != Long.MAX_VALUE) {
            this.high += high;
        }
    }

    public boolean contains(long value) {
        return this.low <= value && value <= this.high;
    }

    public boolean union(Range range) {
        if(range.low < this.low) {
            if(range.high < this.low) {
                return false;
            }
            this.low = range.low;
        }
        if(range.high > this.high) {
            if(range.low > this.high) {
                return false;
            }
            this.high = range.high;
        }
        return true;
    }

    public boolean intersect(Range range) {
        if(range.high < this.low || range.low > this.high) {
            return false;
        }
        if(range.low > this.low) {
            this.low = range.low;
        }
        if(range.high < this.high && range.high >= this.low) {
            this.high = range.high;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Range range) {
            return this.low == range.low && this.high == range.high;
        }
        return false;
    }

    @Override
    public String toString() {
        return super.toString() + '=' + '[' + this.low + ',' + this.high + ']';
    }

    public static Range of(long low, long high) {
        return new Range(low, high);
    }

    public static Range upper(long low) {
        return new Range(low, Long.MAX_VALUE);
    }

    public static Range lower(long high) {
        return new Range(Long.MIN_VALUE, high);
    }

    public static Range full() {
        return new Range(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static Range clone(Range range) {
        return new Range(range.low, range.high);
    }

    public static List<Range> intersect(List<Range> leftRangesOrdered, List<Range> rightRangesOrdered) {
        if(leftRangesOrdered.size() < rightRangesOrdered.size()) {
            var tmp = leftRangesOrdered;
            leftRangesOrdered = rightRangesOrdered;
            rightRangesOrdered = tmp;
        }
        List<Range> result = new ArrayList<>(leftRangesOrdered.size());
        int rightIndex = 0;
        for(Range leftRange : leftRangesOrdered) {
            for(int i = rightIndex; i < rightRangesOrdered.size(); i++) {
                Range range = clone(leftRange);
                Range rightRange = rightRangesOrdered.get(i);
                if(range.intersect(rightRange)) {
                    result.add(range);
                } else {
                    if(leftRange.high < rightRange.low) {
                        break;
                    }
                    if(leftRange.low > rightRange.high) {
                        rightIndex = i + 1;
                    }
                }
            }
        }
        return result;
    }

    public static List<Range> union(List<Range> leftRangesOrdered, List<Range> rightRangesOrdered) {
        List<Range> result = new ArrayList<>(leftRangesOrdered.size() + rightRangesOrdered.size());
        int leftIndex = 0;
        boolean leftLow = true;
        int rightIndex = 0;
        boolean rightLow = true;
        int state = 0;
        var current = Long.MAX_VALUE;
        while (leftIndex < leftRangesOrdered.size() && rightIndex < rightRangesOrdered.size()) {
            Range leftRange = leftRangesOrdered.get(leftIndex);
            var leftValue = leftLow ? leftRange.low : leftRange.high;
            Range rightRange = rightRangesOrdered.get(rightIndex);
            var rightValue = rightLow ? rightRange.low : rightRange.high;
            if(leftValue < rightValue) {
                if(leftLow) {
                    leftLow = false;
                    state += 1;
                    if(leftValue < current) {
                        current = leftValue;
                    }
                } else {
                    leftLow = true;
                    leftIndex += 1;
                    state -= 1;
                    if(state == 0) {
                        Range last = result.isEmpty() ? null : result.getLast();
                        if(!result.isEmpty() &&  last.high == current) {
                            last.high = leftValue;
                        } else {
                            result.add(Range.of(current, leftValue));
                        }
                        current = Long.MAX_VALUE;
                    }
                }
            } else {
                if(rightLow) {
                    rightLow = false;
                    state += 1;
                    if(rightValue < current) {
                        current = rightValue;
                    }
                } else {
                    rightLow = true;
                    rightIndex += 1;
                    state -= 1;
                    if(state == 0) {
                        Range last = result.isEmpty() ? null : result.getLast();
                        if(last != null && last.high == current) {
                            last.high = rightValue;
                        } else {
                            result.add(Range.of(current, rightValue));
                        }
                        current = Long.MAX_VALUE;
                    }
                }
            }
        }
        if(leftIndex < leftRangesOrdered.size()) {
            if(!leftLow) {
                Range leftRange = leftRangesOrdered.get(leftIndex);
                Range last = result.isEmpty() ? null : result.getLast();
                if(last != null && last.high == current) {
                    last.high = leftRange.high;
                } else {
                    result.add(Range.of(current, leftRange.high));
                }
                leftIndex++;
            }
            for(int i = leftIndex; i < leftRangesOrdered.size(); ++i) {
                result.add(leftRangesOrdered.get(i));
            }
        }
        if(rightIndex < rightRangesOrdered.size()) {
            if(!rightLow) {
                Range rightRange = rightRangesOrdered.get(rightIndex);
                Range last = result.isEmpty() ? null : result.getLast();
                if(last != null && last.high == current) {
                    last.high = rightRange.high;
                } else {
                    result.add(Range.of(current, rightRange.high));
                }
                rightIndex++;
            }
            for(int i = rightIndex; i < rightRangesOrdered.size(); ++i) {
                result.add(rightRangesOrdered.get(i));
            }
        }
        return result;
    }
}