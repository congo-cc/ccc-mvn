package org.congocc.testing.maven.plugin.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.congocc.testing.maven.plugin.simple2.ParseException;
import org.congocc.testing.maven.plugin.simple2.Simple2Parser;
import org.congocc.testing.maven.plugin.simple2.Token;
import org.junit.jupiter.api.Test;

/**
 * Tests the parser generated from {@code Grammar2.ccc}, which pulls in {@code Grammar.lexer.ccc} and
 * {@code Common.part.ccc} via {@code INCLUDE}. This proves that the plugin passes fragment files
 * ({@code {grammar}.{part}.ccc}) to the main grammar instead of generating them on their own.
 */
public class Grammar2ParserTest {

    @Test
    public void includedLexerTokensAreAvailable() {
        Simple2Parser parser = new Simple2Parser("42.5");

        Token token = parser.getNextToken();

        assertEquals(Token.TokenType.NUMBER, token.getType());
        assertEquals("42.5", token.toString());
    }

    @Test
    public void parsesBooleanRoot() {
        Simple2Parser parser = new Simple2Parser("false");

        parser.Root();

        assertEquals(Token.TokenType.EOF, parser.getNextToken().getType());
    }

    @Test
    public void numberIsNotAValidRoot() {
        Simple2Parser parser = new Simple2Parser("42");

        assertThrows(ParseException.class, parser::Root);
    }
}
