package cat.nyaa.playtimetracker.condition;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;

import static cat.nyaa.playtimetracker.condition.ConditionTokenizer.*;

public class ConditionExpressionCompiler<T> {

    private final Int2ObjectMap<OperatorProperty> operatorProperties;
    private final Map<String, IParametricVariable<T>> variables;
    private final long precision;

    public ConditionExpressionCompiler(Map<String, IParametricVariable<T>> variables, long precision) {
        this.operatorProperties = new Int2ObjectOpenHashMap<>();
        {
            this.operatorProperties.put(TOKEN_OP_CMP_EQ, new OperatorProperty(8, 2, ComparisonOperator.EQUAL, null));
            //this.operatorProperties.put(TOKEN_OP_CMP_NE, new OperatorProperty(9, 2, ComparisonOperator.NOT_EQUAL, null));
            this.operatorProperties.put(TOKEN_OP_CMP_GT, new OperatorProperty(9, 2, ComparisonOperator.GREATER_THAN, null));
            this.operatorProperties.put(TOKEN_OP_CMP_GE, new OperatorProperty(9, 2, ComparisonOperator.GREATER_THAN_OR_EQUAL, null));
            this.operatorProperties.put(TOKEN_OP_CMP_LT, new OperatorProperty(9, 2, ComparisonOperator.LESS_THAN, null));
            this.operatorProperties.put(TOKEN_OP_CMP_LE, new OperatorProperty(9, 2, ComparisonOperator.LESS_THAN_OR_EQUAL, null));
            this.operatorProperties.put(TOKEN_OP_LOGIC_AND, new OperatorProperty(4, 2, null, LogicalOperator.AND));
            this.operatorProperties.put(TOKEN_OP_LOGIC_OR, new OperatorProperty(3, 2, null, LogicalOperator.OR));
            //this.operatorProperties.put(TOKEN_OP_LOGIC_NOT, new OperatorProperty(13, 1, null, LogicalOperator.NOT));
        }
        this.variables = variables;
        this.precision = precision;
    }

    public ConditionNode<T> compile(ConditionTokenizer.Reader reader) throws CompileException {
        List<Pair<Token, OperatorProperty>> stackOperators = new ObjectArrayList<>();
        List<Object> stack = new ObjectArrayList<>(); // ToLongFunction: variable, Long: constant, ConditionNode: node
        while (reader.hasNext()) {
            Token token = reader.next();
            if (token == null) {
                throw new CompileException("tokenization failed", reader.getException());
            }
            switch (token.type()) {
                case TOKEN_UNKNOWN -> {
                    throw new CompileException("unknown token");
                }
                case TOKEN_END -> {
                    while (!stackOperators.isEmpty()) {
                        var operator = stackOperators.removeLast();
                        if(operator.first().type() == TOKEN_BLOCK_LEFT_PARENTHESES) {
                            throw new CompileException("unmatched left parentheses", operator.first());
                        }
                        var node = processOperator(stack, operator.first(), operator.second());
                        stack.addLast(node);
                    }
                    if (stack.size() != 1) {
                        throw new CompileException("unexpected end of expression: stack size = " + stack.size());
                    }
                    Object result = stack.removeLast();
                    if (!(result instanceof ConditionNode)) {
                        throw new CompileException("unexpected result type: " + result.getClass());
                    }
                    return (ConditionNode<T>) result;
                }
                case TOKEN_VARIABLE -> {
                    stack.addLast(resolveVariable(token));
                }
                case TOKEN_LITERAL_NUMERIC -> {
                    stack.addLast(resolveConstant(token));
                }
                case TOKEN_BLOCK_LEFT_PARENTHESES -> {
                    stackOperators.addLast(Pair.of(token, null));
                }
                case TOKEN_BLOCK_RIGHT_PARENTHESES -> {
                    while (!stackOperators.isEmpty()) {
                        var operator = stackOperators.removeLast();
                        if(operator.first().type() == TOKEN_BLOCK_LEFT_PARENTHESES) {
                            break;
                        }
                        var node = processOperator(stack, operator.first(), operator.second());
                        stack.addLast(node);
                    }
                }
                default -> {
                    OperatorProperty property = operatorProperties.get(token.type());
                    if(property == null) {
                        throw new CompileException("unknown operator: " + token);
                    }
                    while (!stackOperators.isEmpty()) {
                        var topOp = stackOperators.getLast();
                        if(topOp.second() == null || topOp.second().precedence < property.precedence) {
                            break;
                        }
                        stackOperators.removeLast();
                        var node = processOperator(stack, topOp.first(), topOp.second());
                        stack.addLast(node);
                    }
                    stackOperators.addLast(Pair.of(token, property));
                }
            }
        }
        throw new CompileException("unexpected end of expression");
    }

    private ICondition<T> processOperator(List<Object> stack, Token operator, OperatorProperty property) throws CompileException {
        if (property.consume == 2) {
            try {
                Object right = stack.removeLast();
                Object left = stack.removeLast();
                if (property.cmp != null) {
                    ComparisonOperator cmp = property.cmp;
                    if(left instanceof Long) {
                        var tmp = left;
                        left = right;
                        right = tmp;
                        cmp = cmp.exchange();
                    }
                    var condition = new Condition<>(cmp, (IParametricVariable<T>) left, (Long) right, this.precision);
                    return condition;
                }
                if (property.logic != null) {
                    return new ConditionNode<>(property.logic, (ICondition<T>) left, (ICondition<T>) right);
                }
            } catch (RuntimeException e) {
                throw new CompileException("operator " + operator + " failed", operator, e);
            }
        }
        throw new UnsupportedOperationException();
    }

    private IParametricVariable<T> resolveVariable(Token token) throws CompileException {
        IParametricVariable<T> variable = variables.get(token.value());
        if (variable == null) {
            throw new CompileException("variable not found: " + token.value());
        }
        return variable;
    }

    private Long resolveConstant(Token token) throws CompileException {
        if(token.type() == TOKEN_LITERAL_NUMERIC) {
            String s = token.value();
            int i = s.length();
            while (i > 1 && !Character.isDigit(s.charAt(i - 1))) {
                --i;
            }

            String number = i < s.length() ? s.substring(0, i) : s;
            long valueLong = 0;
            double valueDouble = 0;
            try {
                valueLong = Long.parseUnsignedLong(number);
            } catch (NumberFormatException e) {
                try {
                    valueDouble = Double.parseDouble(number);
                    if(valueDouble < 0) {
                        throw new CompileException("negative number", token);
                    }
                } catch (NumberFormatException e2) {
                    throw new CompileException("invalid number", token, e2);
                }
            }

            long factor = 1;
            if (i < s.length()) {
                String unit = s.substring(i);
                factor = switch (unit) {
                    case "ms" -> 1;
                    case "s" -> 1000;
                    case "m", "min" -> 60_000;
                    case "h", "hour" -> 3_600_000;
                    case "d", "day" -> 86_400_000;
                    default -> throw new CompileException("unknown unit", token);
                };
            }

            if (valueLong > 0) {
                return valueLong * factor;
            }
            if (valueDouble > 0) {
                return (long) (valueDouble * (double) factor);
            }
        }
        throw new UnsupportedOperationException();
    }


    public static final class CompileException extends Exception {

        public final @Nullable Token token;

        public CompileException(String message) {
            super(message);
            this.token = null;
        }

        public CompileException(String message, @Nullable Token token) {
            super(message);
            this.token = token;
        }

        public CompileException(String message, Throwable cause) {
            super(message, cause);
            this.token = null;
        }

        public CompileException(String message, @Nullable Token token, Throwable cause) {
            super(message, cause);
            this.token = token;
        }

        @Override
        public String getMessage() {
            if(token != null) {
                return super.getMessage() + " with " + token;
            } else {
                return super.getMessage();
            }
        }
    }


    private record OperatorProperty(int precedence, int consume, @Nullable ComparisonOperator cmp, @Nullable LogicalOperator logic) {
    }
}
