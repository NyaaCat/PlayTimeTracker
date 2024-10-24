package cat.nyaa.playtimetracker.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.ToLongFunction;

public class TestLexer {

    @Test
    public void testLexer() throws Exception {
        var p = new ConditionTokenizer();
        var lexer = p.parse ("a1 >1&&( 2>=b2 ||c!=-3ms) && !d");
        ConditionTokenizer.Token token = null;
        while (lexer.hasNext()) {
            token = lexer.next();
            System.out.println(token);
            Assertions.assertNotNull(token, () -> lexer.getException().toString());
        }
        Assertions.assertEquals(ConditionTokenizer.TOKEN_END, token.type());

        var lexer2 = p.parse ("a1 >1000&&( 2>=b2 ||c==3s)");
        Map<String, IParametricVariable<Long>> variables = Map.of(
                "a1", new SimpleVariable("a1"),
                "b2", new SimpleVariable("b2"),
                "c", new SimpleVariable("c")
        );
        var compiler = new ConditionExpressionCompiler<>(variables, 1000);
        var exp = compiler.compile(lexer2);
        Assertions.assertNotNull(exp);
    }
}
