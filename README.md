# CongoCC Maven Plugin (ccc-mvn)
This Maven plugin allows to run [CongoCC](https://parsers.org/) to generate source code from CongoCC grammer files as part of the Maven build cycle.

# Example
```xml
<plugin>
  <groupId>org.congocc</groupId>
  <artifactId>org.congocc.maven.plugin</artifactId>
  <version>2.0.0</version>
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

# Note
`build-helper-maven-plugin` might be needed after this goal to add the directory where the generated source code is located as a new source code directory, for example like [this](https://github.com/eclipse-daanse/org.eclipse.daanse.mdx/blob/01ae414ae7ebff7dd7acbd86d8842b79a10d1079/parser.ccc/pom.xml#L49-L67).
      
