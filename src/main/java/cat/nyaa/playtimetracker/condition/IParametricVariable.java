package cat.nyaa.playtimetracker.condition;

import java.util.List;

public interface IParametricVariable<T> {

    /**
     * @return the name of the variable
     */
    String name();

    /**
     * @param source the data source to get the value from
     * @return the value of the variable
     */
    long getValue(final T source);

    /**
     * to solve the parametric equation
     * $$
     * variable(t) \in target
     * $$
     *
     * @param target the target value; this will be consumed by the method so don't reuse it
     * @param source the data source to get the value from
     * @return the solution of the equation [t]; or empty if the equation is unsolvable
     */
    default List<Range> resolve(Range target, final T source) {
        long current = getValue(source);
        target.offset(-current);
        return List.of(target);
    }
}
