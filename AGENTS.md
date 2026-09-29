# AI Agent Guidelines

This document outlines the guidelines for AI agents interacting with this repository. Adhering to these principles
ensures consistent, safe, and efficient collaboration.

## Purpose

The primary purpose of an AI agent in this repository is to assist human developers by automating tasks, providing
insights, and maintaining code quality, while strictly following established project conventions and best practices.

## Documentation

Before working on any area of the codebase, read the relevant doc in `docs/dev/`:

| Area                             | Document                                                       |
|-----------------------------------|-----------------------------------------------------------------|
| Machine processing framework      | [docs/dev/machine-framework.md](docs/dev/machine-framework.md)  |
| Energy system                     | [docs/dev/energy-system.md](docs/dev/energy-system.md)          |
| Generators                        | [docs/dev/generators.md](docs/dev/generators.md)                |
| JEI integration                   | [docs/dev/jei-integration.md](docs/dev/jei-integration.md)      |
| Build system and Gradle setup     | [docs/dev/build-system.md](docs/dev/build-system.md)            |
| Configuration options             | [docs/dev/configuration.md](docs/dev/configuration.md)          |
| Dependency versions               | [docs/dev/dependencies.md](docs/dev/dependencies.md)            |
| Troubleshooting common issues     | [docs/dev/troubleshooting.md](docs/dev/troubleshooting.md)      |

## Project Structure

```text
steves-simple-machines/
  settings.gradle, gradle.properties   Root project is inert: includes core/neoforge/gametest, no root build.gradle
  gradlew, gradlew.bat, gradle/         Gradle wrapper

  buildSrc/                       Convention plugins shared by core, neoforge, and gametest
    src/main/groovy/
      ssm.conventions.gradle      Bare java + ssm.repositories + JUnit platform (no NeoForge)
      ssm.neoforge-mod.gradle     NeoForge-specific convention: net.neoforged.moddev + client/server runs + JEI
                                   compileOnly deps; applied by neoforge/ and gametest/, not core/
      ssm.repositories.gradle     Maven repos: NeoForged, Mojang, Jared (JEI), Modrinth

  core/                            Platform-agnostic module: neoFormVersion only (vanilla MC classes, no
                                    NeoForge APIs)
    build.gradle                   Applies ssm.conventions + net.neoforged.moddev directly (neoFormVersion, not
                                    full NeoForge); exposes a `coreOutput` consumable configuration
    src/main/java/io/github/scuba10steve/ssm/
      ref/RefStrings.java          MODID constant ("ssm")
      machine/                     MachineRecipeLookup, MachineRecipeResult, SsmMachineRecipe, SmelterAdapter,
                                    MachineInventory (inventory abstraction), ProcessingMachineState (depends on
                                    MachineInventory instead of NeoForge's ItemStackHandler directly)
      generator/                   CombustionBurnState, AdjacentWaterCheck, WindExposureCheck
    src/test/java/io/github/scuba10steve/ssm/   19 unit tests: SmeltingLookupTest, ProcessingMachineStateTest,
                                    CombustionBurnStateTest, AdjacentWaterCheckTest, WindExposureCheckTest,
                                    FakeMachineInventory (test fixture)

  neoforge/                        NeoForge runtime module — everything not listed under core/ above
    build.gradle                   Applies ssm.neoforge-mod (full modded classpath); a `coreClasses` resolvable
                                    configuration consumes core's `coreOutput` and folds it into the jar; JEI
                                    runtimeOnly dep; depends on :core
    src/main/java/io/github/scuba10steve/ssm/
      Ssm.java                     @Mod entry point; registers everything on the mod event bus
      config/SsmConfig.java        ModConfigSpec; written to config/ssm-common.toml
      energy/SimpleEnergyStorage.java  Shared FE buffer used by every machine and generator
      registry/                    DeferredRegister holders: ModBlocks, ModItems, ModBlockEntities,
                                    ModMenuTypes, ModRecipes, ModCreativeTabs, ModCapabilities, ModClientSetup
      recipe/                      CrushingRecipe, SawingRecipe (Smelter reuses vanilla minecraft:smelting)
      machine/                     MachineConfig, MachineConfigs (CRUSHER, SMELTER, SAWMILL),
                                    ProcessingMachineBlock, ProcessingMachineBlockEntity, ProcessingMachineMenu,
                                    ProcessingMachineScreen, ItemStackHandlerInventory (adapter wrapping a real
                                    ItemStackHandler to implement core's MachineInventory)
      generator/                   GeneratorBlock<T> + abstract GeneratorBlockEntity base (FE buffer,
                                    push-to-neighbor loop, energy NBT, ContainerData lo/hi helpers), plus each of
                                    the 4 generators' BlockEntity/Menu/Screen: Combustion, SolarPanel, WaterWheel,
                                    WindTurbine
      compat/jei/                  SsmJeiPlugin, CrushingCategory, SawingCategory (see jei-integration.md)
    src/main/resources/
      META-INF/neoforge.mods.toml
      data/ssm/recipe/              crushing/, sawing/, smelting/, crafting/
      assets/ssm/textures/gui/      Shared processing-machine GUI texture
    src/test/java/io/github/scuba10steve/ssm/   9 unit tests: SimpleEnergyStorageTest, CrushingRecipeTest,
                                    SawingRecipeTest

  gametest/                        Separate Gradle subproject depending on both :core and :neoforge
                                    (ModDevGradle's moddev plugin needs one neoForge{} extension per project, so
                                    this can't be a source set of either)
    build.gradle                   Applies ssm.neoforge-mod; adds :core's and :neoforge's main sourceSets to the
                                    ssm mod
    src/main/java/io/github/scuba10steve/ssm/gametest/
                                    CrusherGameTest, SawmillGameTest, SmelterGameTest, GeneratorGameTest,
                                    BlockDropGameTest (24 gametests total)

  docs/dev/                        This documentation
  AGENTS.md, README.md, CHANGELOG.md, cliff.toml
  .github/workflows/ci.yml         build + gametest on every push
```

## Build And Verification Commands

```bash
# Build the mod (produces the installable jar + runs unit tests)
./gradlew build

# Unit tests only
./gradlew test

# Run NeoForge GameTests (full game test server startup, separate subproject)
./gradlew :gametest:runGameTestServer

# Full verification, matching CI
./gradlew build :gametest:runGameTestServer

# Local manual testing
./gradlew runClient
./gradlew runServer
```

**After code changes, run at least `./gradlew build` before claiming completion; run
`./gradlew :gametest:runGameTestServer` as well for any change touching a machine or generator block entity.**

## Key Codebase Conventions

- **Multi-module split:** Like `steves-simple-storage`, this project has a `core`/`neoforge` split — `core` is
  platform-agnostic (built against `neoFormVersion`: vanilla MC classes only, no NeoForge APIs) and `neoforge` holds
  everything that needs real NeoForge APIs (registration, block entities, menus/screens, config, JEI compat).
  `neoforge` depends on `core` via a `coreOutput`/`coreClasses` Gradle configuration pair. `gametest/` is a separate
  Gradle subproject depending on both, purely because ModDevGradle requires one `neoForge {}` extension per project.
- **Machine framework over new classes:** Adding a 4th, 5th, ... processing machine should almost never require a new
  `BlockEntity`, `Menu`, or `Screen` class. It requires a new `MachineRecipeLookup` + `MachineConfig` entry in
  `MachineConfigs`, new registry entries (`ModBlockEntities`, `ModMenuTypes`, `ModBlocks`, `ModItems`), and a new
  recipe type only if an existing one (vanilla or `ssm`) doesn't already fit. See
  [docs/dev/machine-framework.md](docs/dev/machine-framework.md).
- **Testable pure logic:** State-machine and condition-check logic that doesn't need a live `Level` is extracted into
  small, pure, unit-testable classes (`ProcessingMachineState`, `CombustionBurnState`, `AdjacentWaterCheck`,
  `WindExposureCheck`) rather than being buried inside a `BlockEntity#tick`. Follow this pattern for new logic.
- **Energy is push-based:** Generators push FE to neighbors on their own tick via
  `Capabilities.EnergyStorage.BLOCK`; machines only ever receive. See
  [docs/dev/energy-system.md](docs/dev/energy-system.md).
- **Registration flow:** New blocks/items/entities/menus/recipes follow the existing `DeferredRegister` patterns in
  `neoforge/src/main/java/io/github/scuba10steve/ssm/registry/`.
- **JEI is compile/runtime-only:** JEI API is `compileOnly`, the JEI jar itself is `runtimeOnly` — the mod must
  compile and run without JEI installed. See [docs/dev/jei-integration.md](docs/dev/jei-integration.md).

## Core Mandates

1. **Adherence to Conventions:** Always prioritize and rigorously adhere to existing project conventions (code style,
   naming, architecture, documentation) when reading or modifying code. Analyze surrounding code, tests, and
   configuration files (e.g., `build.gradle`, `gradle.properties`) to understand established patterns.
2. **Library/Framework Verification:** Never assume a library or framework is available or appropriate. Verify its
   established usage within the project by checking imports, configuration files, or observing neighboring files
   before employing it.
3. **Style & Structure Mimicry:** Mimic the style (formatting, naming), structure, framework choices, typing, and
   architectural patterns of existing code in the project.
4. **Idiomatic Changes:** Understand the local context (imports, functions/classes) to ensure changes integrate
   naturally and idiomatically.
5. **Comments:** Add code comments sparingly. Focus on *why* something is done, especially for complex logic, rather
   than *what* is done. Do not edit comments separate from the code being changed.
6. **Proactiveness:** Fulfill user requests thoroughly, including adding tests for new features or bug fixes.
   Consider all created files (especially tests) as permanent artifacts unless instructed otherwise.
7. **Confirm Ambiguity/Expansion:** Do not take significant actions beyond the clear scope of the request without
   confirming with the user.
8. **Security:** Prioritize security best practices. Never introduce code that exposes sensitive information.
9. **No Unrequested Commits:** **NEVER** stage or commit changes unless explicitly instructed by the user.
10. **No Attribution on Commits:** **NEVER** add a Co-Authored section to a commit.

## Tool Usage

* **File System Operations:** Use `read_file`, `write_file`, and `replace` tools for file manipulation. Always read a
  file before modifying it to ensure accuracy.
* **Code Search:** Utilize `search_file_content` for efficient codebase searching.
* **Shell Commands:** Use `run_shell_command` for executing shell commands. Always explain the purpose and potential
  impact of commands that modify the file system or system state before execution. Prefer non-interactive commands.
* **Efficient Output:** When using `run_shell_command`, prefer flags that reduce output verbosity. Redirect large
  outputs to temporary files if necessary.

## Version & Release Management

* **Version location:** `mod_version` in `gradle.properties` is the single source of truth for the mod's version
  (currently `0.1.0`).
* **No release automation yet:** Unlike `steves-simple-storage`, this repository does not yet have `bump-version` or
  `release` GitHub Actions workflows — `.github/workflows/ci.yml` only builds and runs the gametest suite on every
  push. Until such workflows exist, bump `mod_version` manually as part of a release commit, tag the commit
  (`vX.Y.Z`), and record the change in `CHANGELOG.md` (see `cliff.toml` for the changelog generation convention).
* **If/when release automation is added,** mirror `steves-simple-storage`'s `bump-version.yml` / `release.yml`
  pattern rather than inventing a new one, and update this section accordingly.
