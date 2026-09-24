# Troubleshooting Guide

Common issues encountered developing and running Steve's Simple Machines, and how to diagnose them.

## Build Issues

### Gradle dependency resolution errors

```
Could not resolve all files for configuration ':compileClasspath'
```

1. Check internet connectivity — the first build downloads NeoForge, Minecraft, and JEI artifacts.
2. `./gradlew build --refresh-dependencies`
3. Verify the repositories in `buildSrc/src/main/groovy/ssm.repositories.gradle` are reachable (NeoForged, Mojang,
   Jared, Modrinth).

### Java version mismatch

```
Unsupported class file major version
```

1. Ensure Java 21 is installed and active (`java -version`).
2. Set `JAVA_HOME` to a Java 21 install.
3. Confirm with `./gradlew -version`.

### "cannot enable modding in the same project twice"

This NeoGradle error means `neoForge { version = ... }` was set more than once for the same Gradle project.
`gametest/build.gradle` already gets modding enabled via `ssm.conventions`; its own `neoForge {}` block must only
*extend* the existing `runs`/`mods` configuration (adding the `gameTestServer` run and the root project's source
set to the `ssm` mod), never re-assign `version`.

## GameTest Issues

### `runGameTestServer` reports 0 tests, or the wrong tests

Gametests are gated by `neoforge.enabledGameTestNamespaces`, set in `gametest/build.gradle`:

```gradle
systemProperty 'neoforge.enabledGameTestNamespaces', 'ssm'
```

If a new `@GameTest`-annotated class lives in a different namespace, or the structure template's resource location
uses a different namespace than `ssm`, the test silently won't run. Check the console log line
`Enabled Gametest Namespaces: [ssm]` at startup.

### Gametest can't find game content (blocks, recipes)

`gametest/` is a separate Gradle project with its own `sourceSets.main`, which contains only test code — no blocks,
items, or recipes of its own. The root project's actual game content only becomes visible to the gametest server
because `gametest/build.gradle` explicitly adds it:

```gradle
mods {
    ssm {
        modSourceSets.add(providers.provider { project(':').sourceSets.main })
    }
}
```

If this line is ever removed or the root project's source set changes shape, gametests will fail to find blocks
registered in the root project's `ModBlocks`.

## Unit Test Issues

### A registry tag (e.g. `FluidTags.WATER`) is empty in a unit test

`neoForge.unitTest` boots far enough to statically register built-in objects (`Fluids.WATER`, etc.) but does **not**
run the datapack/resource reload that normally populates tag membership on world load. A test exercising
production code that checks `SomeState.is(SomeTag)` will see an empty tag unless the test binds it manually first,
the way `AdjacentWaterCheckTest` does:

```java
@BeforeAll
static void bindWaterTagForTest() {
    BuiltInRegistries.FLUID.bindTags(Map.of(
            FluidTags.WATER, List.of(Fluids.WATER.builtInRegistryHolder(), Fluids.FLOWING_WATER.builtInRegistryHolder())));
}
```

Prefer this over weakening production code to check fluid/item identity instead of tag membership — the tag check
is the behaviorally correct one (it also matches modded water-like fluids), it's just not automatically populated
by the unit test harness.

### `SmeltingLookupTest`-style live-`RecipeManager` cases

Recipe lookups that go through a live `Level`'s `RecipeManager` (as `MachineConfigs.SMELTER`'s lookup does) can't be
fully exercised in a unit test without a loaded world's recipe data; where that matters, prefer testing the pure
adapter/logic pieces directly (e.g. `SmelterAdapter`) and defer the full end-to-end path to the gametest suite.

## Machine and Generator Behavior

### A machine's progress bar is stuck at 100% and not resetting

This is expected, not a bug: `ProcessingMachineState` **parks** at `progress == processTicks` when the output slots
don't have room for the recipe's outputs, and stops charging energy while parked (see
[docs/dev/machine-framework.md](docs/dev/machine-framework.md)). Empty an output slot and the machine will complete
the operation on its next tick.

### A generator seems to "waste" fuel/sunlight/wind when its buffer is full

For the Solar Panel, Water Wheel, and Wind Turbine, this can't happen — `SimpleEnergyStorage.generate(...)` simply
clamps to remaining capacity and returns 0 if there's no room; the underlying condition (daylight, adjacent water,
clear sky) isn't "consumed" by a tick that generates nothing.

The Combustion Generator is the one case where consuming a *resource* (burn time) while the buffer is full would be
wasteful, and it's handled explicitly: `CombustionBurnState.tick` returns early without decrementing
`remainingBurnTime` when `bufferRoom <= 0`. This is an intentional divergence from `steves-advanced-storage`'s
`CoalGeneratorBlockEntity`, which does burn down (and waste) fuel with a full buffer — see
[docs/dev/generators.md](docs/dev/generators.md) for the full comparison.

### Energy pushed to a neighbor isn't showing up

Confirm the neighbor's `BlockEntityType` actually has `Capabilities.EnergyStorage.BLOCK` registered in
`ModCapabilities` — a `BlockEntity` with an `energyStorage` field but no `registerBlockEntity(...)` call is invisible
to `level.getCapability(Capabilities.EnergyStorage.BLOCK, ...)` and will never receive a push (see
[docs/dev/energy-system.md](docs/dev/energy-system.md)).

## JEI Integration Issues

### A new machine's recipes don't show up in JEI

1. If it introduces a new recipe type, confirm both `registerCategories` and `registerRecipes` were updated in
   `SsmJeiPlugin` — a category alone with no recipes registered against it renders empty.
2. If it reuses an existing recipe type (vanilla or `ssm`), it needs **only** a `registerRecipeCatalysts` entry
   pointing at that type — see the Smelter's catalyst-only pattern in
   [docs/dev/jei-integration.md](docs/dev/jei-integration.md). Adding a redundant category for an already-covered
   recipe type causes duplicate JEI entries, not missing ones.

### JEI integration breaks the build/runtime when JEI isn't present

It shouldn't — JEI is `compileOnly` + `runtimeOnly` only (see
[docs/dev/jei-integration.md](docs/dev/jei-integration.md)). If a compile error or a `NoClassDefFoundError` appears
without JEI installed, something outside `compat/jei/` is importing a JEI class unconditionally; move that usage
behind the same dependency boundary.

## Asset Gaps

As of this writing, block/item **models, blockstates, and language (`lang`) files have not yet been authored** —
only a single shared processing-machine GUI texture exists under `assets/ssm/textures/gui/`. Running `runClient`
will show missing-texture blocks and raw translation keys (e.g. `block.ssm.crusher`) instead of rendered
blocks/labels; this is a known asset gap, not a build or logic defect, and does not affect unit tests or gametests
(neither depends on rendered assets).

## Getting Help

When reporting an issue, include:

1. The full stack trace and relevant `logs/latest.log` entries.
2. Steps to reproduce.
3. Minecraft/NeoForge/mod versions (`minecraft_version`, `neoforge_version`, `mod_version` in `gradle.properties`).
4. Whether JEI was installed, and its version.

## Community Resources

- **NeoForge Documentation**: https://docs.neoforged.net/
- **Minecraft Wiki**: https://minecraft.wiki/
- **ModDevGradle**: https://github.com/neoforged/ModDevGradle
- **JEI Documentation**: https://github.com/mezz/JustEnoughItems
