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

The base convention plugin, applied (directly or transitively) by all three projects — deliberately bare (`java` +
`ssm.repositories` + `tasks.withType(Test).configureEach { useJUnitPlatform() }`), with no NeoForge dependency at
all, so `core/build.gradle` can layer a `neoFormVersion`-only `neoForge {}` block on top of it without conflict.

### `buildSrc/src/main/groovy/ssm.neoforge-mod.gradle`

The NeoForge-specific convention plugin, applied by `neoforge/` and `gametest/` (not `core/`). Applies
`net.neoforged.moddev` + `ssm.conventions`, sets `version`/`group`, pins the Java 21 toolchain, sets
`neoForge.version = neoforge_version` with `client`/`server` runs, declares the JEI API jars `compileOnly` plus
`implementation project(':core')`, and JUnit `testImplementation`/`testRuntimeOnly`.

Both `neoforge/build.gradle` and `gametest/build.gradle` apply this and then add their own `mods { ssm { ... } }`
block on top — each project enables modding (`neoForge.version = ...`) exactly once, satisfied here, so their own
blocks only *extend* `runs`/`mods` rather than re-setting `version` (re-setting it would trip NeoGradle's "cannot
enable modding in the same project twice" guard; see [troubleshooting.md](./troubleshooting.md)).

### `core/build.gradle`

Applies `net.neoforged.moddev` directly (not through `ssm.neoforge-mod`) with only `neoFormVersion` set — that's
what keeps `core` platform-agnostic: it gives vanilla `net.minecraft` classes without any NeoForge API. Sets
`base.archivesName = 'ssm-core'`. Because ModDevGradle only wires that vanilla classpath onto the `main` source
set's `compileClasspath`, `core/build.gradle` also has `configurations.testCompileClasspath.extendsFrom(...)` /
`testRuntimeClasspath.extendsFrom(...)` lines so tests (`AdjacentWaterCheckTest`, `SmeltingLookupTest`, etc.) can
see `net.minecraft` classes too. It exposes a `coreOutput` configuration (`canBeConsumed = true`, backed by
`compileJava.destinationDirectory` and the processed resources dir) — a Gradle
[variant-aware dependency](https://docs.gradle.org/current/userguide/cross_project_publications.html) that lets
`neoforge` (and, transitively, `gametest`) consume `core`'s compiled classes and resources without publishing an
actual artifact.

### `neoforge/build.gradle`

Applies `ssm.neoforge-mod`, sets `base.archivesName = 'ssm'`. Declares a `coreClasses` resolvable configuration
consuming `core`'s `coreOutput`, and `jar { from configurations.coreClasses }` folds `core`'s compiled
classes/resources directly into the `neoforge` jar — so the built mod is a single self-contained jar even though
the source lives in two Gradle modules. Also declares `neoForge { mods { ssm { ... } } }` (adding `core`'s main
source set via `modSourceSets`) and, inside that same block, `unitTest { enable(); testedMod = mods.ssm }` —
ModDevGradle's support for running plain JUnit tests against a mod-loaded classpath (needed because
`CrushingRecipeTest`/`SawingRecipeTest` touch Minecraft classes like `ItemStack`). This has to live here, after
`mods.ssm` exists, rather than in `ssm.neoforge-mod`, since `mods.ssm` isn't declared until each applying project's
own `build.gradle` runs. `processResources` expands `${mod_version}`/`${loader_version_range}`/
`${neoforge_version_range}`/`${minecraft_version_range}`/`${jei_version_range}` tokens into
`META-INF/neoforge.mods.toml`. The `runtimeOnly` JEI dependency lives only here, not in the shared convention
plugin — see [jei-integration.md](./jei-integration.md) for why `gametest/` deliberately doesn't get it.

### `gametest/build.gradle`

Applies `ssm.neoforge-mod`, plus `evaluationDependsOn(':core')` and `evaluationDependsOn(':neoforge')` — needed
because IDE Gradle-sync resolves the `modSourceSets.add(providers.provider { ... })` lazy providers below eagerly,
before those projects have configured, in a way `./gradlew build` does not (see
[troubleshooting.md](./troubleshooting.md)). Adds its own `neoForge { runs { gameTestServer { ... } } }` and `mods {
ssm { ... } }` block: `ssm.neoforge-mod` already enables modding (with a version) for this project, so this block
only *adds to* that — re-setting `neoForge.version` here would trip the same "cannot enable modding in the same
project twice" guard mentioned above. The two `modSourceSets.add(...)` calls (one for `:core`, one for `:neoforge`)
are what make both `core`'s pure logic and `neoforge`'s actual game content (blocks, recipes, etc.) visible to the
gametest server, since `gametest/`'s own source set contains only test code, no game content of its own. Also
declares `implementation project(':core')` and `implementation project(':neoforge')`.

### `gradle.properties`

Holds every version and range token referenced above: `minecraft_version`/`minecraft_version_range`,
`neoforge_version`/`neoforge_version_range`, `loader_version_range`, `jei_version`/`jei_version_range`, and the
`mod_id`/`mod_name`/`mod_license`/`mod_version`/`mod_authors`/`mod_description` values `processResources` expands
into `neoforge.mods.toml`.

### `settings.gradle`

`rootProject.name = 'steves-simple-machines'` plus `include 'core'`, `include 'neoforge'`, `include 'gametest'`,
and the standard `pluginManagement` block (Gradle Plugin Portal + the NeoForged Maven repo, for
`org.gradle.toolchains.foojay-resolver-convention`).

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
