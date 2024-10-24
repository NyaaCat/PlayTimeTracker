package cat.nyaa.playtimetracker.condition;

import java.util.List;

public interface ICondition<T> {

    /**
     * Test if the condition is satisfied.
     *
     * @param source the binding parameters
     * @return true if the condition is satisfied
     */
    boolean test(T source);

    /**
     * Resolve the condition equation
     *
     * @param source the binding parameters
     * @return a list of ranges FOLLOW THE ORDER OF NUMBERS, or empty list if the condition equation is unsolvable
     */
    List<Range> resolve(T source);
}
