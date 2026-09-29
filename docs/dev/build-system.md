# Build System Configuration

## Overview

Steve's Simple Machines uses a **`core`/`neoforge` multi-module Gradle project** with **ModDevGradle
(`net.neoforged.moddev`) 2.0.141**. Like `steves-simple-storage`, `core` is a platform-agnostic module built against
`neoFormVersion` (vanilla MC classes only, no NeoForge APIs), and `neoforge` is the NeoForge runtime module that
depends on it. There is no root `build.gradle` — the root project only wires `settings.gradle`, `gradle.properties`,
and `buildSrc/`. The third Gradle project in this repository is `gametest/`, which exists purely because ModDevGradle
requires one `neoForge {}` extension per project; it is a build-tooling necessity, not an additional platform
boundary, and it depends on both `:core` and `:neoforge`.

## Requirements

- **Java 21** (required for NeoForge 1.21.1)
- **Gradle 9.x** (provided by the wrapper — `./gradlew`)
- Internet access for the first build (downloads NeoForge, JEI, and Minecraft artifacts)

## Project Structure

```
steves-simple-machines/
├── settings.gradle           # rootProject.name, includes 'core', 'neoforge', 'gametest'
├── gradle.properties         # minecraft_version, neoforge_version, mod_version, mod_id, etc.
├── gradlew / gradlew.bat
├── buildSrc/                 # Convention plugins, applied across core/, neoforge/, and gametest/
│   ├── build.gradle          #   groovy-gradle-plugin + moddev-gradle dependency
│   └── src/main/groovy/
│       ├── ssm.conventions.gradle    # java + ssm.repositories + JUnit platform (no NeoForge)
│       ├── ssm.neoforge-mod.gradle   # net.neoforged.moddev + ssm.conventions + client/server runs + JEI
│       │                             #   compileOnly deps — applied by neoforge/ and gametest/, not core/
│       └── ssm.repositories.gradle   # Maven repos: NeoForged, Mojang, Jared (JEI), Modrinth
├── core/
│   ├── build.gradle          # Applies ssm.conventions + net.neoforged.moddev directly, with neoFormVersion
│   │                          #   (vanilla MC classes, no NeoForge APIs); exposes `coreOutput`
│   └── src/
│       ├── main/java/        # Platform-agnostic mod code (see AGENTS.md for the file list)
│       └── test/java/        # Unit tests (19 tests)
├── neoforge/
│   ├── build.gradle          # Applies ssm.neoforge-mod, sets archivesName, JEI runtimeOnly,
│   │                          #   neoforge.mods.toml token substitution, consumes core's `coreOutput` via
│   │                          #   a `coreClasses` configuration folded into the jar
│   └── src/
│       ├── main/java/        # NeoForge-specific mod code (registry, machine BE/Menu/Screen, generators, JEI)
│       ├── main/resources/   # neoforge.mods.toml, data/, assets/
│       └── test/java/        # Unit tests (9 tests)
├── gametest/
│   ├── build.gradle          # Applies ssm.neoforge-mod; adds :core's and :neoforge's main sourceSets to the
│   │                          #   ssm mod; defines the gameTestServer run
│   └── src/main/java/        # GameTest classes (24 gametests)
└── docs/                     # This documentation
```

## Build Configuration

### `buildSrc/src/main/groovy/ssm.conventions.gradle`

The base convention plugin, applied (directly or transitively) by all three projects — deliberately bare, with no
NeoForge dependency at all, so `core/build.gradle` can layer a `neoFormVersion`-only `neoForge {}` block on top of it
without conflict:

```gradle
plugins {
    id 'java'
    id 'ssm.repositories'
}

tasks.withType(Test).configureEach {
    useJUnitPlatform()
}
```

### `buildSrc/src/main/groovy/ssm.neoforge-mod.gradle`

The NeoForge-specific convention plugin, applied by `neoforge/` and `gametest/` (not `core/`):

```gradle
plugins {
    id 'java'
    id 'net.neoforged.moddev'
    id 'ssm.conventions'
}

version = rootProject.mod_version
group = 'io.github.scuba10steve'

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = neoforge_version

    runs {
        client { client() }
        server { server() }
    }
}

dependencies {
    compileOnly "mezz.jei:jei-1.21.1-common-api:19.27.0.340"
    compileOnly "mezz.jei:jei-1.21.1-neoforge-api:19.27.0.340"
    implementation project(':core')

    testImplementation 'org.junit.jupiter:junit-jupiter:5.14.4'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}
```

Both `neoforge/build.gradle` and `gametest/build.gradle` apply this and then add their own `mods { ssm { ... } }`
block on top — each project enables modding (`neoForge.version = ...`) exactly once, satisfied here, so their own
blocks only *extend* `runs`/`mods` rather than re-setting `version` (re-setting it would trip NeoGradle's "cannot
enable modding in the same project twice" guard; see [troubleshooting.md](./troubleshooting.md)).

### `core/build.gradle`

```gradle
plugins {
    id 'net.neoforged.moddev'
    id 'ssm.conventions'
}

version = rootProject.mod_version
group = 'io.github.scuba10steve'

base {
    archivesName = 'ssm-core'
}

neoForge {
    // neoFormVersion gives us vanilla MC classes only — no NeoForge APIs
    neoFormVersion = "${minecraft_version}-20240808.144430"
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.14.4'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

configurations.testCompileClasspath.extendsFrom(configurations.compileClasspath)
configurations.testRuntimeClasspath.extendsFrom(configurations.runtimeClasspath)

test {
    useJUnitPlatform()
}

configurations {
    coreOutput {
        canBeConsumed = true
        canBeResolved = false
    }
}
artifacts {
    coreOutput(compileJava.destinationDirectory) { builtBy compileJava }
    coreOutput(sourceSets.main.output.resourcesDir) { builtBy processResources }
}
```

`core` applies `net.neoforged.moddev` directly (not through `ssm.neoforge-mod`) with only `neoFormVersion` set, which
gives vanilla `net.minecraft` classes without any NeoForge API — that's what keeps `core` platform-agnostic. Because
ModDevGradle only wires that vanilla classpath onto the `main` source set's `compileClasspath`, the
`testCompileClasspath`/`testRuntimeClasspath` `extendsFrom` calls are needed so tests (`AdjacentWaterCheckTest`,
`SmeltingLookupTest`, etc.) can see `net.minecraft` classes too. The `coreOutput` configuration is a Gradle
[variant-aware dependency](https://docs.gradle.org/current/userguide/cross_project_publications.html) that exposes
`core`'s compiled classes and resources for `neoforge` (and, transitively, `gametest`) to consume without publishing
an actual artifact.

### `neoforge/build.gradle`

```gradle
plugins {
    id 'ssm.neoforge-mod'
}

base {
    archivesName = 'ssm'
}

neoForge {
    mods {
        ssm {
            sourceSet sourceSets.main
            modSourceSets.add(providers.provider { project(':core').sourceSets.main })
        }
    }

    unitTest {
        enable()
        testedMod = mods.ssm
    }
}

configurations {
    coreClasses {
        canBeConsumed = false
        canBeResolved = true
    }
}

jar {
    from configurations.coreClasses
}

processResources {
    def replaceProperties = [
        mod_version: mod_version,
        loader_version_range: loader_version_range,
        neoforge_version_range: neoforge_version_range,
        minecraft_version_range: minecraft_version_range
    ]
    inputs.properties replaceProperties
    filesMatching('META-INF/neoforge.mods.toml') {
        expand(replaceProperties)
    }
}

dependencies {
    runtimeOnly "mezz.jei:jei-1.21.1-neoforge:19.27.0.340"
    coreClasses project(path: ':core', configuration: 'coreOutput')
}
```

The `coreClasses` resolvable configuration consumes `core`'s `coreOutput` and `jar { from configurations.coreClasses }`
folds `core`'s compiled classes/resources directly into the `neoforge` jar, so the built mod is a single self-contained
jar even though the source lives in two Gradle modules. `unitTest.enable()` is ModDevGradle's support for running
plain JUnit tests against a mod-loaded classpath (needed because `CrushingRecipeTest`/`SawingRecipeTest` touch
Minecraft classes like `ItemStack`); it's declared here, after `mods.ssm` exists, rather than in `ssm.neoforge-mod`,
since `mods.ssm` isn't declared until each applying project's own `build.gradle` runs. The `runtimeOnly` JEI
dependency lives only here, not in the shared convention plugin — see [jei-integration.md](./jei-integration.md) for
why `gametest/` deliberately doesn't get it.

### `gametest/build.gradle`

```gradle
plugins {
    id 'ssm.neoforge-mod'
}

evaluationDependsOn(':core')
evaluationDependsOn(':neoforge')

neoForge {
    runs {
        gameTestServer {
            type = "gameTestServer"
            systemProperty 'neoforge.enabledGameTestNamespaces', 'ssm'
        }
    }

    mods {
        ssm {
            sourceSet sourceSets.main
            modSourceSets.add(providers.provider { project(':core').sourceSets.main })
            modSourceSets.add(providers.provider { project(':neoforge').sourceSets.main })
        }
    }
}

dependencies {
    implementation project(':core')
    implementation project(':neoforge')
    testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.14.4"
}
```

`ssm.neoforge-mod` already enables modding (with a version) for this project; this block only *adds to* that —
re-setting `neoForge.version` here would trip the same "cannot enable modding in the same project twice" guard
mentioned above. The two `modSourceSets.add(...)` calls are what make both `core`'s pure logic and `neoforge`'s actual
game content (blocks, recipes, etc.) visible to the gametest server, since `gametest/`'s own source set contains only
test code, no game content of its own.

### `gradle.properties`

```properties
minecraft_version=1.21.1
minecraft_version_range=[1.21.1,1.22)
neoforge_version=21.1.226
neoforge_version_range=[21.1,22)
loader_version_range=[4,)

mod_id=ssm
mod_name=Steve's Simple Machines
mod_license=MIT
mod_version=0.1.0
mod_authors=scuba10steve, zerofall, SBlectric
mod_description=Better power generation and automated ore/wood processing for Minecraft.
```

### `settings.gradle`

```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { name = 'NeoForged'; url = 'https://maven.neoforged.net/releases' }
    }
    plugins {
        id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention'
}

rootProject.name = 'steves-simple-machines'
include 'core'
include 'neoforge'
include 'gametest'
```

## Gradle Tasks

```bash
# Build the mod (produces the installable jar, runs unit tests)
./gradlew build

# Unit tests only
./gradlew test

# Run the NeoForge GameTest suite (separate subproject; full game test server startup)
./gradlew :gametest:runGameTestServer

# Full verification, matching .github/workflows/ci.yml
./gradlew build :gametest:runGameTestServer

# Local manual testing
./gradlew runClient
./gradlew runServer

# Clean build artifacts
./gradlew clean

# Build without daemon (CI)
./gradlew build --no-daemon
```

## Build Outputs

- **Installable jar:** `neoforge/build/libs/ssm-<version>.jar` (contains `core`'s compiled classes/resources too,
  folded in via the `coreClasses`/`jar { from ... }` wiring above)
- **`core`'s own jar:** `core/build/libs/ssm-core-<version>.jar` (an intermediate artifact — not the distributable
  mod jar; nothing outside this build consumes it directly, `neoforge` consumes `core`'s classes/resources via the
  `coreOutput` configuration instead)
- **Unit test reports:** `core/build/reports/tests/test/index.html` and `neoforge/build/reports/tests/test/index.html`
- **GameTest run:** `gametest/build/moddev/` (working directory for the gameTestServer run); no persistent report is
  produced beyond the console log (`All N required tests passed`).

## Continuous Integration

`.github/workflows/ci.yml` runs on every push to any branch: checks out, sets up Java 21 (Temurin), runs `./gradlew
build`, then `./gradlew :gametest:runGameTestServer`, and uploads `**/build/reports/tests/` as an artifact. It does
not currently gate on a specific branch or run on pull requests separately.

## Troubleshooting

See [troubleshooting.md](./troubleshooting.md) for build failures, missing symbols, and gametest
issues.
