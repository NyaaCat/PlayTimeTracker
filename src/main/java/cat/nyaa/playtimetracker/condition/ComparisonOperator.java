package cat.nyaa.playtimetracker.condition;

public enum ComparisonOperator {

    EQUAL,

    // NOT_EQUAL,

    GREATER_THAN,

    GREATER_THAN_OR_EQUAL,

    LESS_THAN,

    LESS_THAN_OR_EQUAL;

    public ComparisonOperator exchange() {
        return switch (this) {
            case EQUAL -> EQUAL;
            case GREATER_THAN -> LESS_THAN;
            case GREATER_THAN_OR_EQUAL -> LESS_THAN_OR_EQUAL;
            case LESS_THAN -> GREATER_THAN;
            case LESS_THAN_OR_EQUAL -> GREATER_THAN_OR_EQUAL;
        };
    }
}
