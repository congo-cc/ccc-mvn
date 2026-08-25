package org.congocc.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "ccc-generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GeneratorMojo extends AbstractMojo {

    @Parameter(property = "sourceDirectory", defaultValue = "${project.basedir}/src/main/congocc")
    Path sourceDirectory;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/congocc")
    Path outputDirectory;

    @Parameter(property = "lang", defaultValue = "java")
    String lang;

    @Parameter(property = "jdk", defaultValue = "8")
    int jdk;

    @Parameter(property = "quiet", defaultValue = "false")
    boolean quiet;

    /**
     * A list of glob patterns (relative to {@code sourceDirectory}) specifying which grammar files to include.
     * Patterns follow the {@code glob:} syntax of {@link java.nio.file.FileSystem#getPathMatcher}.
     * Defaults to {@code **&#47;*.ccc} (all {@code .ccc} files in any subdirectory).
     *
     * <p>Example POM configuration:
     * <pre>{@code
     * <includes>
     *   <include>**&#47;*.ccc</include>
     * </includes>
     * }</pre>
     */
    @Parameter
    List<String> includes;

    /**
     * A list of glob patterns (relative to {@code sourceDirectory}) specifying which grammar files to exclude.
     * Patterns follow the {@code glob:} syntax of {@link java.nio.file.FileSystem#getPathMatcher}.
     * Defaults to {@code **&#47;*.*.ccc}, which excludes grammar fragment files such as
     * {@code grammar.lexer.ccc}, {@code grammar.part.ccc}, or {@code grammar.injects.ccc}.
     *
     * <p>Example POM configuration:
     * <pre>{@code
     * <excludes>
     *   <exclude>**&#47;*.*.ccc</exclude>
     * </excludes>
     * }</pre>
     */
    @Parameter
    List<String> excludes;

    private final Map<String, String> symbols = new HashMap<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (!Files.isDirectory(sourceDirectory)) {
                return;
            }

            List<PathMatcher> includeMatchers = toMatchers(effectiveIncludes());
            List<PathMatcher> excludeMatchers = toMatchers(effectiveExcludes());

            boolean compile = hasChangesToCompile(includeMatchers);
            if (compile) {
                List<Path> grammars = findGrammarFiles(includeMatchers, excludeMatchers);
                getLog().info(grammars.size() + " grammar(s) found > " + grammars.stream().map(p -> p.toFile().getName()).collect(Collectors.joining(", ")));

                for (Path path : grammars) {
                    int returnValue = org.congocc.app.Main.mainProgram(path, outputDirectory, lang, jdk,
                            quiet, symbols);
                    getLog().info("return: " + returnValue);
                }
            } else {
                getLog().info("Thera are not changes to compile ");
            }
            this.project.addCompileSourceRoot(outputDirectory.normalize().toString());
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("CongoCC failure", e);
        }
    }

    private List<String> effectiveIncludes() {
        return includes != null && !includes.isEmpty() ? includes : Collections.singletonList("**/*.ccc");
    }

    private List<String> effectiveExcludes() {
        return excludes != null && !excludes.isEmpty() ? excludes : Collections.singletonList("**/*.*.ccc");
    }

    private List<PathMatcher> toMatchers(List<String> patterns) {
        return patterns.stream()
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .collect(Collectors.toList());
    }

    private boolean matchesAny(Path path, List<PathMatcher> matchers) {
        return matchers.stream().anyMatch(m -> m.matches(path));
    }

    private List<Path> findGrammarFiles(List<PathMatcher> includeMatchers, List<PathMatcher> excludeMatchers) throws IOException {
        return Files.walk(sourceDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> matchesAny(path, includeMatchers))
                .filter(path -> !matchesAny(path, excludeMatchers))
                .collect(Collectors.toList());
    }

    private boolean hasChangesToCompile(List<PathMatcher> includeMatchers) throws IOException {
        long sourceTimestamp = Files.walk(sourceDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> matchesAny(path, includeMatchers))
                .map(path -> path.toFile().lastModified())
                .max(Long::compareTo).orElse(-1L);
        long generatedTimestamp = Files.isDirectory(outputDirectory)
                ? Files.walk(outputDirectory).map(path -> path.toFile().lastModified()).max(Long::compareTo).orElse(-1L)
                : -1;
        return sourceTimestamp > generatedTimestamp;
    }
}
