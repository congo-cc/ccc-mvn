# CongoCC Maven Plugin (ccc-mvn)
This Maven plugin allows to run [CongoCC](https://parsers.org/) to generate source code from CongoCC grammer files as part of the Maven build cycle.

# Example
```xml
<plugin>
  <groupId>org.congocc</groupId>
  <artifactId>org.congocc.maven.plugin</artifactId>
  <version>2.1.0</version>
  <executions>
    <execution>
      <goals>
        <goal>ccc-generate</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <sourceDirectory>${project.basedir}/src/main/ccc</sourceDirectory> <!-- default value: ${project.basedir}/src/main/congocc -->
    <lang>java</lang> <!-- default value: java, supported values: java, python, csharp -->
    <jdk>17</jdk>     <!-- default value: 8 -->
    <quiet>false</quiet>  <!-- default value: false -->
    <outputDirectory>${project.basedir}/src/gen/java</outputDirectory> <!-- default value: ${project.build.directory}/generated-sources/congocc -->
  </configuration>
</plugin>
```
Full `pom.xml` examples can be found [here](https://github.com/congo-cc/ccc-mvn/tree/main/org.congocc.testing.maven.plugin.simple) and [here](https://github.com/eclipse-daanse/org.eclipse.daanse.mdx/blob/main/parser.ccc/pom.xml).

# Notes
* The `outputDirectory` is registered as a compile source root automatically, so `build-helper-maven-plugin` is not needed.
* Files named `{grammar}.{part}.ccc` (for example `Grammar.lexer.ccc`) are treated as fragments for `INCLUDE` and are not generated on their own.
* Generation is skipped when no grammar file is newer than the generated sources. Run `mvn clean` to force regeneration.
* A non-zero exit code from CongoCC fails the build.

# Snapshots
Every push to `main` publishes the current development version (see the `revision` property in `pom.xml`) to the Central Portal snapshot repository. To use it, add the repository to your `pom.xml` or `settings.xml`:
```xml
<pluginRepositories>
  <pluginRepository>
    <id>central-portal-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <releases><enabled>false</enabled></releases>
    <snapshots><enabled>true</enabled></snapshots>
  </pluginRepository>
</pluginRepositories>
```
Snapshots are overwritten on every push and deleted after 90 days. Use a released version for anything that must stay reproducible.

# Releasing
See [RELEASING.md](RELEASING.md). Releases are created from the GitHub *Releases* page and published to Maven Central by CI.
      
