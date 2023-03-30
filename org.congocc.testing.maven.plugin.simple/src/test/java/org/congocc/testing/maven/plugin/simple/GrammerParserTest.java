package org.congocc.testing.maven.plugin.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class GrammerParserTest {

    private static final String TRUE = "true";

    @Test
    public void test() {
        SimpleParser parser = new SimpleParser(TRUE);

        String token = parser.getNextToken()
                .getImage();
        assertEquals(TRUE, token);

    }

}
