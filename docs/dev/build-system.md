# Build System Configuration

## Overview

Steve's Simple Machines uses a **single-module Gradle project** with **ModDevGradle (`net.neoforged.moddev`) 2.0.141**.
Unlike `steves-simple-storage`, there is no `core`/`neoforge` split — all mod code lives directly under the root
project's `src/main/java`. The only other Gradle project in this repository is `gametest/`, which exists purely
because ModDevGradle requires one `neoForge {}` extension per project; it is a build-tooling necessity, not a
platform-abstraction boundary, and it depends on the root project (`implementation project(':')`).

## Requirements

- **Java 21** (required for NeoForge 1.21.1)
- **Gradle 9.x** (provided by the wrapper — `./gradlew`)
- Internet access for the first build (downloads NeoForge, JEI, and Minecraft artifacts)

## Project Structure

```
steves-simple-machines/
├── build.gradle              # Root project: applies ssm.conventions, sets archivesName, JEI runtimeOnly,
│                              #   neoforge.mods.toml token substitution
├── settings.gradle           # rootProject.name, includes 'gametest'
├── gradle.properties         # minecraft_version, neoforge_version, mod_version, mod_id, etc.
├── gradlew / gradlew.bat
├── buildSrc/                 # Convention plugins, applied by both the root project and gametest/
│   ├── build.gradle          #   groovy-gradle-plugin + moddev-gradle dependency
│   └── src/main/groovy/
│       ├── ssm.conventions.gradle    # java + net.neoforged.moddev + ssm.repositories + JUnit platform
│       └── ssm.repositories.gradle   # Maven repos: NeoForged, Mojang, Jared (JEI), Modrinth
├── src/
│   ├── main/java/            # All mod code (machine, generator, energy, registry, compat/jei, ...)
│   ├── main/resources/       # neoforge.mods.toml, data/, assets/
│   └── test/java/            # Unit tests (22 tests)
├── gametest/
│   ├── build.gradle          # Applies ssm.conventions; adds the root project's main sourceSet to the
│   │                          #   ssm mod; defines the gameTestServer run
│   └── src/main/java/        # GameTest classes (14 gametests)
└── docs/                     # This documentation
```

## Build Configuration

### `buildSrc/src/main/groovy/ssm.conventions.gradle`

The shared convention plugin, applied by both the root project and `gametest/`:

```gradle
plugins {
    id 'java'
    id 'net.neoforged.moddev'
    id 'ssm.repositories'
}

version = rootProject.mod_version
group = 'io.github.scuba10steve.ssm'

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = neoforge_version

    runs {
        client { client() }
        server { server() }
    }

    mods {
        ssm {
            sourceSet sourceSets.main
        }
    }

    unitTest {
        enable()
        testedMod = mods.ssm
    }
}

dependencies {
    compileOnly "mezz.jei:jei-1.21.1-common-api:19.27.0.340"
    compileOnly "mezz.jei:jei-1.21.1-neoforge-api:19.27.0.340"

    testImplementation 'org.junit.jupiter:junit-jupiter:5.14.4'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.withType(Test).configureEach { useJUnitPlatform() }
```

`unitTest.enable()` is ModDevGradle's support for running plain JUnit tests against a mod-loaded classpath (needed
because `ProcessingMachineStateTest` and friends touch Minecraft classes like `ItemStack`/`Level`), rather than a
plain vanilla `neoFormVersion`-only test setup.

### Root `build.gradle`

```gradle
plugins {
    id 'ssm.conventions'
}

base { archivesName = 'ssm' }

dependencies {
    runtimeOnly "mezz.jei:jei-1.21.1-neoforge:19.27.0.340"
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
```

The `runtimeOnly` JEI dependency lives only here, not in the shared convention plugin — see
[docs/dev/jei-integration.md](docs/dev/jei-integration.md) for why `gametest/` deliberately doesn't get it.

### `gametest/build.gradle`

```gradle
plugins {
    id 'ssm.conventions'
}

evaluationDependsOn(':')

version = rootProject.mod_version
group = 'io.github.scuba10steve.ssm.test'

neoForge {
    runs {
        gameTestServer {
            type = "gameTestServer"
            systemProperty 'neoforge.enabledGameTestNamespaces', 'ssm'
        }
    }

    mods {
        ssm {
            modSourceSets.add(providers.provider { project(':').sourceSets.main })
        }
    }
}

dependencies {
    implementation project(':')
    testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.14.4"
}
```

`ssm.conventions` already enables modding (with a version) for this project and adds `gametest`'s own `sourceSets.main`
to the `ssm` mod; this block only *adds to* that — re-setting `neoForge.version` here would trip NeoGradle's "cannot
enable modding in the same project twice" guard. The `modSourceSets.add(...)` call is what makes the root project's
actual game content (blocks, recipes, etc.) visible to the gametest server, since `gametest/`'s own source set
contains only test code, no game content of its own.

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

- **Installable jar:** `build/libs/ssm-<version>.jar`
- **Unit test reports:** `build/reports/tests/test/index.html`
- **GameTest run:** `gametest/build/moddev/` (working directory for the gameTestServer run); no persistent report is
  produced beyond the console log (`All N required tests passed`).

## Continuous Integration

`.github/workflows/ci.yml` runs on every push to any branch: checks out, sets up Java 21 (Temurin), runs `./gradlew
build`, then `./gradlew :gametest:runGameTestServer`, and uploads `**/build/reports/tests/` as an artifact. It does
not currently gate on a specific branch or run on pull requests separately.

## Troubleshooting

See [docs/dev/troubleshooting.md](docs/dev/troubleshooting.md) for build failures, missing symbols, and gametest
issues.
