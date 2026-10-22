package org.congocc.maven.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratorMojoOldTest {

    private static final String GRAMMAR = ""
            + "BASE_NAME=\"Simple\";\n"
            + "PARSER_PACKAGE=\"simple\";\n"
            + "SKIP : <WHITESPACE : (\" \"| \"\\t\"| \"\\n\"| \"\\r\")+>;\n"
            + "TOKEN : <TRUE: \"true\"> | <FALSE: \"false\">;\n"
            + "Value : <TRUE> | <FALSE>;\n"
            + "Root : Value! <EOF>!;\n";

    private static final String GRAMMAR_WITH_INCLUDE = ""
            + "BASE_NAME=\"Second\";\n"
            + "PARSER_PACKAGE=\"second\";\n"
            + "INCLUDE \"Second.lexer.ccc\"\n"
            + "SKIP : <WHITESPACE : (\" \"| \"\\t\"| \"\\n\"| \"\\r\")+>;\n"
            + "Root : <NUMBER> <EOF>!;\n";

    private static final String LEXER_FRAGMENT = "TOKEN : <NUMBER : ([\"0\"-\"9\"])+>;\n";

    @TempDir
    Path tempDir;

    private Path sourceDirectory;
    private Path outputDirectory;
    private MavenProject project;
    private GeneratorMojo mojo;

    @BeforeEach
    void setUp() throws Exception {
        sourceDirectory = tempDir.resolve("src/main/congocc");
        outputDirectory = tempDir.resolve("target/generated-sources/congocc");
        project = new MavenProject();
        mojo = new GeneratorMojo();
        mojo.sourceDirectory = sourceDirectory;
        mojo.outputDirectory = outputDirectory;
        mojo.includes = Collections.singletonList("**/*.ccc");
        mojo.excludes = Collections.singletonList("**/*.*.ccc");
        mojo.lang = "java";
        mojo.jdk = 8;
        mojo.quiet = true;
        Field projectField = GeneratorMojo.class.getDeclaredField("project");
        projectField.setAccessible(true);
        projectField.set(mojo, project);
    }

    @Test
    void generatesParserAndRegistersSourceRoot() throws Exception {
        writeGrammar("Grammar.ccc", GRAMMAR);

        mojo.execute();

        assertTrue(Files.isRegularFile(outputDirectory.resolve("simple/SimpleParser.java")), "parser generated");
        assertTrue(Files.isRegularFile(outputDirectory.resolve("simple/SimpleLexer.java")), "lexer generated");
        assertEquals(Collections.singletonList(outputDirectory.normalize().toString()), project.getCompileSourceRoots());
    }

    @Test
    void fragmentsAreIncludedButNotGeneratedOnTheirOwn() throws Exception {
        writeGrammar("Second.ccc", GRAMMAR_WITH_INCLUDE);
        writeGrammar("Second.lexer.ccc", LEXER_FRAGMENT);

        mojo.execute();

        assertTrue(Files.isRegularFile(outputDirectory.resolve("second/SecondParser.java")), "parser generated");
        String parser = new String(Files.readAllBytes(outputDirectory.resolve("second/Token.java")), StandardCharsets.UTF_8);
        assertTrue(parser.contains("NUMBER"), "token from included fragment present");
        List<String> packages;
        try (Stream<Path> dirs = Files.list(outputDirectory)) {
            packages = dirs.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
        }
        assertEquals(Collections.singletonList("second"), packages, "only the main grammar is generated");
    }

    @Test
    void missingSourceDirectoryIsANoOp() throws Exception {
        mojo.execute();

        assertFalse(Files.exists(outputDirectory));
        assertTrue(project.getCompileSourceRoots().isEmpty());
    }

    @Test
    void unchangedGrammarIsNotRegenerated() throws Exception {
        Path grammar = writeGrammar("Grammar.ccc", GRAMMAR);
        mojo.execute();
        Path generated = outputDirectory.resolve("simple/SimpleParser.java");
        FileTime generatedAt = FileTime.fromMillis(2_000_000_000_000L);
        Files.setLastModifiedTime(generated, generatedAt);
        Files.setLastModifiedTime(grammar, FileTime.fromMillis(1_000_000_000_000L));

        mojo.execute();

        assertEquals(generatedAt, Files.getLastModifiedTime(generated), "generated file untouched");
        assertEquals(Collections.singletonList(outputDirectory.normalize().toString()), project.getCompileSourceRoots(),
                "source root is registered even when nothing is regenerated");
    }

    @Test
    void changedGrammarIsRegenerated() throws Exception {
        Path grammar = writeGrammar("Grammar.ccc", GRAMMAR);
        mojo.execute();
        Path generated = outputDirectory.resolve("simple/SimpleParser.java");
        Files.setLastModifiedTime(generated, FileTime.fromMillis(1_000_000_000_000L));
        Files.setLastModifiedTime(grammar, FileTime.fromMillis(2_000_000_000_000L));

        mojo.execute();

        assertTrue(Files.getLastModifiedTime(generated).toMillis() > 1_000_000_000_000L, "generated file rewritten");
    }

    @Test
    void invalidGrammarFailsTheBuild() throws Exception {
        writeGrammar("Broken.ccc", "BASE_NAME=\"Broken\";\nRoot : <UNDEFINED_TOKEN> ;\n");

        assertThrows(MojoFailureException.class, mojo::execute);
    }

    private Path writeGrammar(String name, String content) throws IOException {
        Files.createDirectories(sourceDirectory);
        Path file = sourceDirectory.resolve(name);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
