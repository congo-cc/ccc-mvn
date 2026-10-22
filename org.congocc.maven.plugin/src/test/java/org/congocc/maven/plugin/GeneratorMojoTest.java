package org.congocc.maven.plugin;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MojoTest
class GeneratorMojoTest {

    @Test
    @DisplayName("both includes and excludes have their default values when not configured")
    @InjectMojo(goal = "ccc-generate", pom = "src/test/resources/unit/default-config/pom.xml")
    void bothHaveDefaults(GeneratorMojo mojo) {
        assertEquals(1, mojo.includes.size());
        assertEquals("**/*.ccc", mojo.includes.get(0));
        assertEquals(1, mojo.excludes.size());
        assertEquals("**/*.*.ccc", mojo.excludes.get(0));
    }

    @Test
    @DisplayName("both includes and excludes have their default values when not configured")
    @InjectMojo(goal = "ccc-generate", pom = "src/test/resources/unit/default-config/pom.xml")
    @MojoParameter(name = "includes", value = "mygrammar/*.ccc")
    void customIncludes(GeneratorMojo mojo) {
        assertEquals(1, mojo.includes.size());
        assertEquals("mygrammar/*.ccc", mojo.includes.get(0));
        assertEquals(1, mojo.excludes.size());
        assertEquals("**/*.*.ccc", mojo.excludes.get(0));
    }

    @Test
    @DisplayName("both includes and excludes have their default values when not configured")
    @InjectMojo(goal = "ccc-generate", pom = "src/test/resources/unit/default-config/pom.xml")
    @MojoParameter(name = "excludes", value = "**/ignore/*.ccc")
    void customExcludes(GeneratorMojo mojo) {
        assertEquals(1, mojo.includes.size());
        assertEquals("**/*.ccc", mojo.includes.get(0));
        assertEquals(1, mojo.excludes.size());
        assertEquals("**/ignore/*.ccc", mojo.excludes.get(0));
    }

    @Test
    @DisplayName("both includes and excludes have their default values when not configured")
    @InjectMojo(goal = "ccc-generate", pom = "src/test/resources/unit/default-config/pom.xml")
    @MojoParameter(name = "includes", value = "mygrammar/*.ccc")
    @MojoParameter(name = "excludes", value = "**/ignore/*.ccc")
    void bothCustom(GeneratorMojo mojo) {
        assertEquals(1, mojo.includes.size());
        assertEquals("mygrammar/*.ccc", mojo.includes.get(0));
        assertEquals(1, mojo.excludes.size());
        assertEquals("**/ignore/*.ccc", mojo.excludes.get(0));
    }
}
