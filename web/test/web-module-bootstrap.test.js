import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();

test("web bootstrap uses the latest TeaVM web snapshot API", () => {
  const gradle = fs.readFileSync(path.join(root, "web", "build.gradle.kts"), "utf8");
  const builder = fs.readFileSync(
    path.join(root, "web", "src", "main", "java", "com", "unciv", "app", "web", "BuildWebCommon.java"),
    "utf8",
  );
  const launcher = fs.readFileSync(
    path.join(root, "web", "src", "main", "java", "com", "unciv", "app", "web", "WebLauncher.java"),
    "utf8",
  );

  assert.match(gradle, /gdxTeaVMVersion = "-SNAPSHOT"/);
  assert.match(gradle, /backend-web:\$gdxTeaVMVersion/);
  assert.match(gradle, /gdx-freetype-web:\$gdxTeaVMVersion/);
  assert.match(gradle, /JVM_17/);
  assert.match(builder, /com\.github\.xpenatan\.gdx\.teavm\.backends\.web/);
  assert.match(builder, /new WebBackend\(\)/);
  assert.match(launcher, /new WebApplication\(new WebGame\(\), config\)/);
  assert.doesNotMatch(launcher, /TeaApplication/);
});
