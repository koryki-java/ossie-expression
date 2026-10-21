package ai.koryki.ossie;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class OssieParserTest {

    /** Parses the whole input and returns the syntax errors, empty if there are none. */
    private static List<String> errors(String input) {
        List<String> errors = new ArrayList<>();
        BaseErrorListener listener =
                new BaseErrorListener() {
                    @Override
                    public void syntaxError(
                            Recognizer<?, ?> r,
                            Object symbol,
                            int line,
                            int col,
                            String msg,
                            RecognitionException e) {
                        errors.add(line + ":" + col + " " + msg);
                    }
                };
        ossieLexer lexer = new ossieLexer(CharStreams.fromString(input));
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        ossieParser parser = new ossieParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        parser.expression();
        return errors;
    }

    private static ossieParser.ExprContext parse(String input) {
        assertTrue(errors(input).isEmpty(), () -> input + " -> " + errors(input));
        ossieParser parser =
                new ossieParser(
                        new CommonTokenStream(new ossieLexer(CharStreams.fromString(input))));
        return parser.expression().expr();
    }

    private static List<String> lines(String resource) {
        try (var in = OssieParserTest.class.getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .lines()
                    .filter(l -> !l.isBlank() && !l.startsWith("# "))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<DynamicTest> valid(String resource) {
        return lines(resource).stream()
                .map(
                        l ->
                                dynamicTest(
                                        l,
                                        () ->
                                                assertTrue(
                                                        errors(l).isEmpty(),
                                                        () -> errors(l).toString())));
    }

    @TestFactory
    Stream<DynamicTest> specExamplesParse() {
        return valid("spec-examples.txt");
    }

    @TestFactory
    Stream<DynamicTest> apacheOssieExamplesParse() {
        return valid("ossie-examples.txt");
    }

    @TestFactory
    Stream<DynamicTest> invalidExpressionsAreRejected() {
        return lines("invalid.txt").stream()
                .map(l -> dynamicTest(l, () -> assertFalse(errors(l).isEmpty())));
    }

    @Test
    void multiplicationBindsTighterThanAddition() {
        var value = assertInstanceOf(ossieParser.ValueOnlyExprContext.class, parse("1 + 2 * 3"));
        var add = assertInstanceOf(ossieParser.AdditiveValueContext.class, value.valueExpr());
        assertInstanceOf(ossieParser.MultiplicativeValueContext.class, add.valueExpr(1));
    }

    @Test
    void notBindsLooserThanComparison() {
        var not = assertInstanceOf(ossieParser.NotExprContext.class, parse("NOT a = b"));
        assertInstanceOf(ossieParser.ComparisonExprContext.class, not.expr());
    }

    @Test
    void andBindsTighterThanOr() {
        var or = assertInstanceOf(ossieParser.OrExprContext.class, parse("a OR b AND c"));
        assertInstanceOf(ossieParser.AndExprContext.class, or.expr(1));
    }

    @Test
    void betweenKeepsItsAndOutOfTheLogicalAnd() {
        var and =
                assertInstanceOf(
                        ossieParser.AndExprContext.class, parse("x BETWEEN 1 AND 2 AND y"));
        assertInstanceOf(ossieParser.BetweenExprContext.class, and.expr(0));
    }

    @Test
    void keywordsAreCaseInsensitiveIdentifiersAreKept() {
        var ref = parse("Sum(Amount) filter (where Status = 'x')");
        assertInstanceOf(ossieParser.ValueOnlyExprContext.class, ref);
    }
}
