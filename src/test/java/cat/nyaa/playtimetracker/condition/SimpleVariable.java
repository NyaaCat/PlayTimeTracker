package cat.nyaa.playtimetracker.condition;

public class SimpleVariable implements IParametricVariable<Long> {

    public final String name;

    public SimpleVariable(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public long getValue(Long source) {
        return source;
    }

    public Range getValueFromParam(Long source, Range range) {
        range.offset(source);
        return range;
    }
}
