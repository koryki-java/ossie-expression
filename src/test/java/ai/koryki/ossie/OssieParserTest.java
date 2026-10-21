package ai.koryki.ossie;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

class OssieParserTest {

    private static ossieParser parser(String input) {
        ossieParser parser =
                new ossieParser(
                        new CommonTokenStream(new ossieLexer(CharStreams.fromString(input))));
        parser.setErrorHandler(new BailErrorStrategy());
        return parser;
    }

    private static String tree(String input) {
        ossieParser parser = parser(input);
        return parser.expression().expr().toStringTree(parser);
    }

    @Test
    void multiplicationBindsTighterThanAddition() {
        ossieParser.ExprContext root = parser("1 + 2 * 3").expression().expr();
        ossieParser.AdditiveExprContext add =
                assertInstanceOf(ossieParser.AdditiveExprContext.class, root);
        assertInstanceOf(ossieParser.MultiplicativeExprContext.class, add.expr(1));
    }

    @Test
    void parsesFunctionCallOverQualifiedNames() {
        tree("sum(orders.amount) > 100 AND region = 'EU'");
    }

    @Test
    void rejectsIncompleteExpression() {
        assertThrows(ParseCancellationException.class, () -> tree("1 +"));
    }
}
