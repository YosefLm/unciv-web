# FileTextureData reflection contract

This fixture reproduces reflective access used by `TextureData.getReadonlyPixmap()`
and new-game resource options. It checks the pixmap field read/write path, enum
constant lookup, deprecated constant annotations, and `MapResourceSetting`'s
filtered labels using the production `UncivReflectionPolicy` service.

Export the web module's resolved runtime classpath first:

```sh
./gradlew -Pweb -PgdxTeaVMSource=/path/to/gdx-teavm :web:writeValidationClasspath :web:compileJava
```

Run the fixture as a standalone Gradle build with a matched immutable TeaVM fork set:

```sh
gradle jvmContract runJavaScriptContract \
  -PclasslibJar=/path/to/classlib.jar \
  -PcoreJar=/path/to/core.jar \
  -PjsoJar=/path/to/jso.jar
```

The JVM contract checks the JDK reference. The JavaScript contract compiles with
TeaVM and runs in Node using the exported web runtime classpath and reflection
policy service. This focused contract validates metadata selection; it does not
validate graphics rendering or game startup.
