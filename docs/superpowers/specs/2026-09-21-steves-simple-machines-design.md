# Steve's Simple Machines — v1 Design

Status: draft
Date: 2026-09-21

## Overview

A new, standalone NeoForge mod for Minecraft 1.21.1: mod_id `ssm`, mod_name
"Steve's Simple Machines", package `io.github.scuba10steve.ssm`. It adds
powered machines for three player-facing goals: better power generation,
automated ore processing, and automated wood processing.

It is a sibling to `steves-simple-storage` (S3) and `steves-advanced-storage`,
not a companion or dependency of either. It has no hard runtime dependency on
either mod. A future initiative may extract a shared library of common code
(energy helpers, generic machine/GUI framework, shared JEI plumbing) used by
all three mods — that work is out of scope here and is not designed in this
spec; this mod's code should stay reasonably decoupled so that extraction
later is a mechanical lift, not a rewrite, but no shared-library abstraction
is built now.

## Non-goals (v1)

Explicitly deferred to future versions, listed so scope creep during
implementation is easy to catch:

- Thermal/nuclear power generation
- Geothermal/magmatic power generation
- Biodiesel and gasoline generators
- Multi-stage ore enrichment chains (wash/enrich beyond a single crush step)
- Multiblock structures of any kind
- Shared common library extraction across the three mods
- Multi-loader support (Fabric, etc.) — repo structure below is chosen so this
  isn't blocked later, but nothing is built for it now

## Conventions carried from S3

`steves-simple-storage` and `steves-advanced-storage` establish house style
this mod should match where it doesn't conflict with the single-module
decision below:

- `AGENTS.md` at repo root with a "Documentation" table pointing into
  `docs/dev/*.md`, one doc per subsystem
- `docs/dev/*.md` subsystem docs (architecture, build system, configuration,
  dependencies, troubleshooting)
- `CHANGELOG.md` + `cliff.toml` (git-cliff) for changelog generation
- GitHub Actions: `ci.yml` (build/test), plus changelog and version-bump
  workflows
- `gradle.properties` driving mod metadata (`mod_id`, `mod_name`,
  `mod_license`, `mod_version`, `mod_authors`, `mod_description`,
  `minecraft_version`, `minecraft_version_range`, `neoforge_version`,
  `neoforge_version_range`, `loader_version_range`)
- `mod_authors=zerofall, SBlectric` (same authorship as the sibling mods)
- `neoforge_version=21.1.226` / `neoforge_version_range=[21.1,22)` to match
  the sibling mods' toolchain exactly, avoiding a second NeoForge version to
  track locally
- Gametest-based integration tests under a `gametest` module/source set

## Project structure

Single-module NeoForge project (standard MDK layout), **not** S3's
`core/`+`neoforge/` split. That split exists in S3 to keep the door open for a
second loader; this mod has no such requirement yet, and a single module is
simpler to build and reason about. If multi-loader or shared-library work
happens later, splitting a single module into `core`/`neoforge` is a
mechanical refactor (move platform-agnostic classes into a new source set),
not a redesign.

```
steves-simple-machines/
  build.gradle, settings.gradle, gradle.properties, buildSrc/
  gradlew, gradlew.bat, gradle/
  src/main/java/io/github/scuba10steve/ssm/
    Ssm.java                      (mod entrypoint)
    registry/                     (DeferredRegister holders: blocks, items,
                                    block entities, menus, recipe types,
                                    recipe serializers)
    machine/
      ProcessingMachineBlockEntity.java   (generic base, see below)
      ProcessingMachineBlock.java
      ProcessingMachineMenu.java
      ProcessingMachineScreen.java
      MachineConfig.java          (per-machine-type config: slots, energy
                                    cost/tick, process ticks, recipe type)
    recipe/
      CrushingRecipe.java + CrushingRecipeSerializer.java
      SawingRecipe.java + SawingRecipeSerializer.java
      (Smelter deliberately has no custom recipe type — see below)
    generator/
      GeneratorBlockEntity.java   (generic base for passive/fuel generators)
      CombustionGeneratorBlockEntity.java
      SolarPanelBlockEntity.java
      WaterWheelBlockEntity.java
      WindTurbineBlockEntity.java
    energy/
      SimpleEnergyStorage.java    (thin IEnergyStorage impl used by both
                                    machines and generators)
    compat/jei/
      SsmJeiPlugin.java
      CrushingCategory.java
      SawingCategory.java
  src/main/resources/
    data/ssm/recipe/crushing/*.json
    data/ssm/recipe/sawing/*.json
    data/ssm/recipe/smelting/*.json    (type "minecraft:smelting" — needed
                                         for the mod's own new items, e.g.
                                         ssm:iron_crushed_ore → iron ingot;
                                         the Smelter still reuses the vanilla
                                         recipe *type*, it just needs these
                                         extra recipe instances authored)
    assets/ssm/... (models, textures, lang, blockstates)
  src/gametest/java/io/github/scuba10steve/ssm/gametest/
  docs/dev/*.md
  AGENTS.md, README.md, CHANGELOG.md, cliff.toml
  .github/workflows/*.yml
```

## Machine framework

All three v1 processing machines (Crusher, Smelter, Sawmill) are
structurally identical: consume FE per tick while a recipe is active, hold
one input slot and one-or-more output slots, and finish a timed operation to
produce output(s). Rather than three bespoke block entity classes, they share
one generic `ProcessingMachineBlockEntity`, parameterized by a
`MachineConfig` per machine type:

- `recipeType: RecipeType<? extends Recipe<?>>` — which recipe book to query
- `inputSlotCount`, `outputSlotCount`
- `energyCapacity`, `energyPerTick` (FE)
- `baseProcessTicks` (overridable per-recipe if a recipe wants to be slower)

Vanilla `Recipe<T>#assemble` returns a single `ItemStack`, which can't
express the Sawmill's two outputs (planks + sawdust) or a per-recipe
duration override. The two custom recipe types (`ssm:crushing`, `ssm:sawing`)
therefore implement a small `SsmMachineRecipe` interface (output list +
optional duration override) rather than relying on vanilla `Recipe#assemble`
directly; the generic block entity reads through that interface for its own
recipe types. The Smelter is the one exception — it stays behind vanilla's
plain `Recipe<SingleRecipeInput>`/`RecipeType.SMELTING` interface exactly as
shipped, single output included, since that's the whole point of reusing it
unmodified.

`ProcessingMachineBlockEntity` owns: an `ItemStackHandler` for slots, a
`SimpleEnergyStorage` for its FE buffer, and per-tick logic: if enough energy
is buffered and a matching recipe exists for the input slot(s) and the output
slot(s) have room, consume energy and advance progress; on completion, move
input to output per the recipe result(s) and clear/decrement input.

One generic `ProcessingMachineMenu`/`ProcessingMachineScreen` pair renders
slot positions from `MachineConfig`, so adding a fourth machine later means
adding a config + recipe type, not a new GUI class.

Each of the three v1 machines is a thin `ProcessingMachineBlock` +
`MachineConfig` registration; they do not need their own block entity
subclass unless a machine needs behavior the generic base can't express (none
of the three v1 machines do).

Every machine and generator block also needs a standard shaped crafting
recipe so players can obtain it (there is no other acquisition path in v1 —
no world generation, no trading). Exact ingredients are an implementation/
balance detail, not an architectural one, but each of the 7 v1 blocks
(Crusher, Smelter, Sawmill, Combustion Generator, Solar Panel, Water Wheel,
Wind Turbine) must ship one.

### Crusher

- Recipe type `ssm:crushing`, custom `CrushingRecipe`/serializer
- Input: 1 raw ore item (matched via ingredient on the NeoForge common raw-
  materials tag, e.g. `c:raw_materials/iron`, not an ore-block tag); Output:
  1 output slot, yielding 2x the crushed ore/dust item per operation (e.g.
  `minecraft:raw_iron` → 2x `ssm:iron_crushed_ore`, which itself smelts into
  an ingot via a mod-authored `minecraft:smelting`-type recipe — see Smelter)
- Custom recipe type is needed because "double the vanilla smelting input"
  isn't expressible as a reuse of any vanilla recipe type
- v1 ships crushing recipes for the vanilla ores that have raw-ore items:
  iron, gold, copper (raw_iron/raw_gold/raw_copper → crushed variants)

### Smelter

- Deliberately reuses vanilla `minecraft:smelting` recipes directly via
  `RecipeManager`/`RecipeType.SMELTING`, instead of defining a parallel
  `ssm:smelting` recipe type. This means the Smelter automatically works with
  every smeltable item already in the game or added by any other mod (raw
  ore, crushed ore from the Crusher, food, sand→glass, etc.) with zero recipe
  duplication, and picks up mod-added smelting recipes for free.
- Cost/time: v1 uses a flat FE cost and process-tick count independent of the
  vanilla recipe's cook time/XP (matching the vanilla output, ignoring
  vanilla's `cookingTime`/experience fields, since porting XP-on-smelt
  behavior to a machine is out of scope for v1). Furnace fuel items
  (`FuelValues`) are *not* consumed by the Smelter — it runs on FE only.

### Sawmill

- Recipe type `ssm:sawing`, custom `SawingRecipe`/serializer
- Input: 1 log/wood item; Output: 2 output slots — primary planks output at a
  better ratio than vanilla crafting (6 planks per log instead of 4) plus a
  secondary `ssm:sawdust` byproduct item (1 per operation)
- v1 ships sawing recipes for all vanilla log types present in Minecraft
  1.21.1 (oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry,
  crimson stem, warped stem) — pale oak is excluded, it doesn't exist in
  1.21.1
- `ssm:sawdust` is a plain item in v1 with no further recipe use defined yet
  (future: fuel, compost, or a recycled-paper-style craft) — shipping it as an
  inert crafting-adjacent byproduct is acceptable for v1

## Energy system

Uses NeoForge's built-in `IEnergyStorage` capability (FE-compatible units) so
cables/batteries/generators from other installed tech mods interoperate
without any bridge code. `SimpleEnergyStorage` is a small class implementing
`IEnergyStorage` backed by a mutable long/int counter, shared by both
generators (as a source, `canExtract=true`) and machines (as a sink,
`canReceive=true`). Each block entity registers its `SimpleEnergyStorage`
against `Capabilities.EnergyStorage.BLOCK` in a `RegisterCapabilitiesEvent`
listener (NeoForge 1.21.x's block-capability system, not the old
provider/`LazyOptional` model) — this registration is what actually makes
the FE buffer visible to external cables, hoppers-for-energy, and batteries;
implementing the interface alone does not. Values (capacities, generation
rates, machine consumption rates) are exposed via NeoForge config
(`ssm-common.toml`) so
they're tunable without a rebuild, matching the "Configuration" convention
documented in S3.

### Generators (v1) — reuse note

`steves-advanced-storage` already has working `CoalGeneratorBlockEntity` and
`SolarGeneratorBlockEntity` classes (`neoforge/src/main/java/io/github/
scuba10steve/s3/advanced/blockentity/`) implementing exactly this energy
approach: a `GeneratorEnergyStorage extends EnergyStorage` helper with a
`generate(int)` method, `Capabilities.EnergyStorage.BLOCK` registration via
`RegisterCapabilitiesEvent` in the main mod class, push-to-neighbor energy
distribution, config-driven rates/capacity, and NBT persistence. The
Combustion Generator and Solar Panel should be ported and adapted from these
(renamed, re-namespaced to `ssm`, config wired to `ssm-common.toml`) rather
than built from scratch — this is proven code and resolves the capability-
registration question above by example. One intentional behavioral
divergence: the existing Coal Generator wastes excess FE once its buffer
fills mid-burn (keeps decrementing `litTime` regardless); ssm's Combustion
Generator instead pauses burning and preserves remaining burn time when full,
per the buffer-full rule above — don't port that part unchanged.

### Generators (v1)

- **Combustion Generator** — burns solid fuel items (reuses vanilla
  `FuelValues`/furnace fuel registry) to generate FE at a fixed rate per fuel
  burn-tick while a fuel item is present in its single fuel slot; internal FE
  buffer with a config-capped max. When the buffer is full, burning pauses
  (the current fuel item's remaining burn time is preserved, not wasted) and
  resumes once buffer space frees up. Early-game analog to a furnace.
- **Solar Panel** — passively generates FE only during daytime
  (`level.isDay()`) when it has sky access directly above (no blocks between
  it and the sky), at a fixed low rate; produces nothing at night or when
  covered. Single tier in v1 (no upgrade tiers).
- **Water Wheel** — must have at least one directly adjacent block whose
  fluid state is water (source or flowing, either counts) at generation
  time; while that holds, generates FE continuously at a fixed rate. No FE
  once no adjacent block still has a water fluid state (removed, drained, or
  turned to ice).
- **Wind Turbine** — must be placed with a clear vertical column above it to
  the sky (build-height check, similar to the Solar Panel's sky-access check
  but independent of time of day); generates FE continuously at a fixed rate
  while unobstructed, scaling isn't modeled in v1 (flat rate, not
  altitude-scaled).

Water Wheel and Wind Turbine are two separate blocks (not a single block that
adapts to its environment) — they have different placement rules, different
models, and are conceptually distinct early-game generator choices depending
on the player's biome/build.

## JEI integration

- **Crusher** and **Sawmill** get dedicated JEI recipe categories
  (`CrushingCategory`, `SawingCategory`) registered by `SsmJeiPlugin`,
  populated from the mod's own recipe manager entries, each with the
  respective machine block registered as the category's catalyst.
- **Smelter** gets no new JEI category. Since it consumes vanilla
  `minecraft:smelting` recipes unmodified, its recipes already appear under
  JEI's existing vanilla Smelting category; `SsmJeiPlugin` registers the
  Smelter block as an additional catalyst for that existing vanilla category
  (`addRecipeCatalyst`) so players see "craftable in Furnace or Smelter"
  without any duplicated recipe data.
- JEI is an optional, client-only dependency: compile-only + runtime-test in
  Gradle, and `SsmJeiPlugin`/the category classes must not be loaded or
  referenced on a dedicated server without JEI installed. Follow whatever
  pattern S3 already uses for this during implementation rather than
  reinventing it.

## Error handling / edge cases

- No FE buffered / insufficient FE per tick: machine holds progress at its
  current value (does not reset) and resumes once energy is available again.
- No matching recipe for current input: machine sits idle, consumes no
  energy, progress stays at 0.
- Output slot(s) full or incompatible with the pending result: operation
  pauses at 100%-ready-to-complete and does not consume the input until space
  frees up (avoids duplicating results or silently dropping items).
- Generators placed in an invalid/unmet condition (turbine underground, water
  wheel with no adjacent water, solar panel at night or covered) simply
  generate 0 FE — this is not an error state, no crash, no particle spam.
- Fuel item placed in the Combustion Generator that has no configured fuel
  value is rejected by slot predicate (mirrors vanilla furnace behavior).
- Datapack reload or an input-slot change mid-operation: on the next tick,
  the block entity re-validates that its in-progress recipe still matches
  the current input; if not, progress resets to 0 rather than completing a
  stale recipe. The selected recipe/output is otherwise not "locked in"
  until the operation actually completes (consistent with the full-output-
  buffer pause above never having consumed the input yet).

## Testing

- Gametest suite (`src/gametest`) mirroring S3's convention, covering per
  machine: a full end-to-end processing cycle (place block, insert valid
  input + sufficient FE, tick forward, assert output), an insufficient-energy
  case (assert no progress), and a full-output-buffer case (assert pause,
  no item loss). Per generator: a "condition met" case asserting non-zero FE
  output after N ticks, and a "condition unmet" case asserting zero output.
- Unit-level tests (JUnit, no game instance) for `SimpleEnergyStorage`
  (extract/receive bounds, capacity clamping) and recipe matching logic where
  it doesn't require a live `Level`.
- CI: adapt S3's `ci.yml` to run `./gradlew build test runGameTestServer`
  (NeoGradle's actual gametest run task name in 1.21.x). The `src/gametest`
  source set needs explicit Gradle wiring (it is not automatically picked up
  by the MDK) plus `setForceExit false` on the run config so a normal server
  exit after tests complete isn't reported as a failure; gametests also need
  structure templates, which should be copied from S3's existing gametest
  setup rather than authored from scratch.

## Documentation

`docs/dev/` subsystem docs, referenced from `AGENTS.md`'s table like S3 does:

- `docs/dev/machine-framework.md` — `ProcessingMachineBlockEntity`,
  `MachineConfig`, how to add a new machine
- `docs/dev/energy-system.md` — `SimpleEnergyStorage`, FE interop notes
- `docs/dev/generators.md` — per-generator condition/rate logic
- `docs/dev/jei-integration.md` — category registration, catalyst-only
  pattern used for the Smelter
- `docs/dev/build-system.md`, `docs/dev/configuration.md`,
  `docs/dev/dependencies.md`, `docs/dev/troubleshooting.md` — same role as
  in S3

User-facing docs (`docs/usage/*.md` per S3's pattern) covering getting
started, block reference, recipes, and automation are expected but not
detailed further in this spec — they follow directly from the machine list
above once built.
