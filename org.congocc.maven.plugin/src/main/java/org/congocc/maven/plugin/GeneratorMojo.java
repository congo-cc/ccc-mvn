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

    /**
     * The current Maven project.
     */
    @Component
    private MavenProject project;

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

    private Map<String, String> symbols = new HashMap<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            List<Path> grammars = findGrammarFiles();
            getLog().info(+ grammars.size() + " grammar(s) found > " + grammars.stream().map(p -> p.toFile().getName()).collect(Collectors.joining(", ")));
            for(Path path : grammars) {
                int returnValue = org.congocc.app.Main.mainProgram(path, outputDirectory, lang, jdk,
                        quiet, symbols);
                this.project.addCompileSourceRoot(outputDirectory.normalize().toString());
                getLog().info("return: " + returnValue);
            }
        } catch(Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("CongoCC failure", e);
        }
    }

    private List<Path> findGrammarFiles() throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.ccc");
        if(Files.isDirectory(sourceDirectory)) {
            return Files.walk(sourceDirectory).filter(matcher::matches).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
