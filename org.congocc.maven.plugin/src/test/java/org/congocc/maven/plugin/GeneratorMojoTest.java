package org.congocc.maven.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorMojoTest {

    @TempDir
    private Path tempDir;

    private GeneratorMojo mojo() {
        GeneratorMojo mojo = new GeneratorMojo();
        mojo.sourceDirectory = tempDir;
        return mojo;
    }

    @SuppressWarnings("unchecked")
    private List<String> effectiveIncludes(GeneratorMojo mojo) throws Exception {
        Method m = GeneratorMojo.class.getDeclaredMethod("effectiveIncludes");
        m.setAccessible(true);
        return (List<String>) m.invoke(mojo);
    }

    @SuppressWarnings("unchecked")
    private List<String> effectiveExcludes(GeneratorMojo mojo) throws Exception {
        Method m = GeneratorMojo.class.getDeclaredMethod("effectiveExcludes");
        m.setAccessible(true);
        return (List<String>) m.invoke(mojo);
    }

    @SuppressWarnings("unchecked")
    private List<PathMatcher> toMatchers(GeneratorMojo mojo, List<String> patterns) throws Exception {
        Method m = GeneratorMojo.class.getDeclaredMethod("toMatchers", List.class);
        m.setAccessible(true);
        return (List<PathMatcher>) m.invoke(mojo, patterns);
    }

    @SuppressWarnings("unchecked")
    private List<Path> findGrammarFiles(GeneratorMojo mojo, List<PathMatcher> includes, List<PathMatcher> excludes) throws Exception {
        Method m = GeneratorMojo.class.getDeclaredMethod("findGrammarFiles", List.class, List.class);
        m.setAccessible(true);
        return (List<Path>) m.invoke(mojo, includes, excludes);
    }

    private List<Path> findGrammarFiles(GeneratorMojo mojo) throws Exception {
        List<PathMatcher> inc = toMatchers(mojo, effectiveIncludes(mojo));
        List<PathMatcher> exc = toMatchers(mojo, effectiveExcludes(mojo));
        return findGrammarFiles(mojo, inc, exc);
    }

    private void touch(String... relativePaths) throws IOException {
        for (String relativePath : relativePaths) {
            Path path = tempDir.resolve(relativePath);
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }
    }

    @Test
    void defaultIncludesIsAllCcc() throws Exception {
        GeneratorMojo mojo = mojo();
        assertEquals(Collections.singletonList("**/*.ccc"), effectiveIncludes(mojo));
    }

    @Test
    void defaultExcludesIsFragmentPattern() throws Exception {
        GeneratorMojo mojo = mojo();
        assertEquals(Collections.singletonList("**/*.*.ccc"), effectiveExcludes(mojo));
    }

    @Test
    void customIncludesOverridesDefault() throws Exception {
        GeneratorMojo mojo = mojo();
        mojo.includes = Collections.singletonList("**/*.grammar");
        assertEquals(Collections.singletonList("**/*.grammar"), effectiveIncludes(mojo));
    }

    @Test
    void customExcludesOverridesDefault() throws Exception {
        GeneratorMojo mojo = mojo();
        mojo.excludes = Collections.singletonList("**/generated/**");
        assertEquals(Collections.singletonList("**/generated/**"), effectiveExcludes(mojo));
    }

    @Test
    void plainCccFileIsIncluded() throws Exception {
        touch("MyGrammar.ccc");

        List<Path> result = findGrammarFiles(mojo());

        assertEquals(1, result.size());
        assertEquals("MyGrammar.ccc", result.get(0).getFileName().toString());
    }

    @Test
    void fragmentFileIsExcludedByDefault() throws Exception {
        touch("grammar.lexer.ccc", "grammar.part.ccc", "grammar.injects.ccc");

        List<Path> result = findGrammarFiles(mojo());

        assertTrue(result.isEmpty(), "Fragment files must be excluded by default");
    }

    @Test
    void mixedFilesOnlyReturnNonFragments() throws Exception {
        touch("Main.ccc", "Sub.ccc", "Main.lexer.ccc", "Sub.part.ccc");

        List<Path> result = findGrammarFiles(mojo());

        assertEquals(2, result.size());
        List<String> names = result.stream()
                .map(p -> p.getFileName().toString())
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        assertEquals(Arrays.asList("Main.ccc", "Sub.ccc"), names);
    }

    @Test
    void nonCccFilesAreNotIncluded() throws Exception {
        touch("README.md", "config.xml", "grammar.txt");

        List<Path> result = findGrammarFiles(mojo());

        assertTrue(result.isEmpty(), "Only .ccc files should be included");
    }

    @Test
    void filesInSubdirectoriesAreFound() throws Exception {
        touch("sub/dir/Grammar.ccc", "sub/dir/Grammar.lexer.ccc");

        List<Path> result = findGrammarFiles(mojo());

        assertEquals(1, result.size());
        assertEquals("Grammar.ccc", result.get(0).getFileName().toString());
    }

    @Test
    void customIncludePatternRestrictsToMatchingFiles() throws Exception {
        touch("Foo.ccc", "Bar.ccc", "sub/Nested.ccc");

        GeneratorMojo mojo = mojo();
        mojo.includes = Collections.singletonList("**/Foo.ccc");
        mojo.excludes = Collections.emptyList();

        List<String> names = findGrammarFiles(mojo).stream()
                .map(p -> p.getFileName().toString())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        assertEquals(Collections.singletonList("Foo.ccc"), names);
    }

    @Test
    void customExcludePatternRemovesMatchingFiles() throws Exception {
        touch("Generated.ccc", "Manual.ccc");

        GeneratorMojo mojo = mojo();
        mojo.excludes = Collections.singletonList("**/Generated.ccc");

        List<Path> result = findGrammarFiles(mojo);

        assertEquals(1, result.size());
        assertEquals("Manual.ccc", result.get(0).getFileName().toString());
    }

    @Test
    void emptySourceDirectoryReturnsNoFiles() throws Exception {
        List<Path> result = findGrammarFiles(mojo());
        assertTrue(result.isEmpty());
    }
}
