package org.congocc.testing.maven.plugin.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.congocc.testing.maven.plugin.simple.ast.Literal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the parser generated from {@code src/main/congocc/Grammer.ccc} by the plugin during the build.
 */
public class GrammerParserTest {

    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    public void tokenizesBooleanLiterals(String input) {
        SimpleParser parser = new SimpleParser(input);

        Token token = parser.getNextToken();

        assertEquals(input, token.toString());
        assertEquals("true".equals(input) ? Token.TokenType.TRUE : Token.TokenType.FALSE, token.getType());
        assertTrue(token instanceof Literal, "token is created as the #Literal node type");
    }

    @ParameterizedTest
    @ValueSource(strings = { "true", " false ", "\ttrue\n" })
    public void parsesRootWithSurroundingWhitespace(String input) {
        SimpleParser parser = new SimpleParser(input);

        parser.Root();

        assertEquals(Token.TokenType.EOF, parser.getNextToken().getType());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "maybe", "true false" })
    public void rejectsInvalidInput(String input) {
        SimpleParser parser = new SimpleParser(input);

        assertThrows(ParseException.class, parser::Root);
    }
}
