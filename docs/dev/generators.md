# Generators

There are 4 generators, each with its own `Menu`/`Screen` and its own `BlockEntity` subclass (unlike the 3 machines,
which share one implementation — generators differ enough in condition logic and per-tick behavior that a shared
`GeneratorConfig` wasn't worth the indirection). All 4 share `GeneratorBlock<T>` (block-level plumbing:
`newBlockEntity`, `getTicker`, opening the menu on right-click) and the abstract `GeneratorBlockEntity` base class
(the FE buffer, the push-to-neighbor energy pattern described in [energy-system.md](./energy-system.md), the energy
NBT round-trip, and the lo/hi `ContainerData` bit-packing helpers). What differs per generator is its *condition
logic* — what has to be true, each tick, for it to generate — and each one's own `ContainerData` slot layout.

Every generator's condition logic must report "am I generating" (and mark the block entity as changed) based on
whether `GeneratorBlockEntity#generateAndMarkChanged(int)` actually accepted energy that tick, never based on the
environmental condition alone — a full buffer is a legitimate reason to stop reporting "generating," and this
invariant used to be duplicated (and drift) independently in each generator before `GeneratorBlockEntity` existed.

## Combustion Generator

Burns fuel to generate FE at a fixed rate per burn-tick. `getBurnTime(ItemStack)` delegates to NeoForge's
`ItemStack#getBurnTime(RecipeType)` extension method (passing `null`, the same as vanilla's own
`AbstractFurnaceBlockEntity.isFuel`), which resolves against the real furnace fuel registry — the `neoforge:furnace_fuels`
data map plus the `FurnaceFuelBurnTimeEvent` hook — rather than a hardcoded item list. Any vanilla or modded fuel item
(coal, charcoal, coal blocks, sticks, planks, blaze rods, lava buckets, etc.) works as fuel, not just a fixed set of 3
hardcoded items.

Condition logic, in `CombustionGeneratorBlockEntity.tick`:

1. If already burning, tick `CombustionBurnState` (see divergence below) and add any generated energy to the buffer.
2. If **not** burning and the buffer isn't already full, check the fuel slot; if it holds a valid fuel item, start
   burning (`burnState.startBurning(burnTime)`) and consume one fuel item.
3. Push to neighbors (shared pattern).

### Intentional divergence from `steves-advanced-storage`

This block entity is ported from `steves-advanced-storage`'s `CoalGeneratorBlockEntity`
(`neoforge/src/main/java/io/github/scuba10steve/s3/advanced/blockentity/CoalGeneratorBlockEntity.java`), but with one
deliberate behavioral change, isolated in `CombustionBurnState.tick(int generationRate, int bufferRoom)`: when
`bufferRoom <= 0`, it returns immediately without decrementing `remainingBurnTime`, pausing the burn instead of
letting it continue for nothing.

- **`steves-advanced-storage`'s source behavior:** burn time counts down every tick regardless of whether the energy
  buffer has room, so a full buffer means the fuel item is silently wasted while it finishes burning with nothing to
  show for it.
- **`ssm`'s behavior:** when the buffer has no room (`bufferRoom <= 0`), the tick is a no-op — `remainingBurnTime` is
  **not** decremented and no energy is generated. Burning effectively pauses and resumes exactly where it left off
  once a neighbor drains the buffer, so a full output buffer never wastes fuel.

This is covered directly by `CombustionBurnStateTest#burnPausesWhenBufferIsFullAndResumesWithSpace`, which asserts a
tick with `bufferRoom = 0` leaves `remainingBurnTime` unchanged before asserting a following tick with room resumes
normally.

## Solar Generator

Not to be confused with the plain **Solar Panel** item (`ssm:solar_panel`) — a crafting-only component with no
block/placement behavior of its own, used as an ingredient in the Solar Generator's crafting recipe. Only the
Solar Generator block has the generation logic described below.

Condition, in `SolarGeneratorBlockEntity.tick`: generates only when `level.isDay()` **and** there is sky light directly
above the block (`level.getBrightness(LightLayer.SKY, pos.above()) > 0`) — a roof, or being underground, blocks
generation even during the day. `getCurrentRate()` (exposed via `ContainerData` for the screen) reflects the actual
rate only while both conditions hold; it's zeroed the instant either fails, including the edge case where
`generate(...)` accepted 0 because the buffer was already full.

## Water Wheel

Condition, in `WaterWheelBlockEntity.tick`: generates whenever any of the 6 orthogonally adjacent blocks is a water
source or flowing water. The check itself is the pure, unit-tested `AdjacentWaterCheck.hasAdjacentWater(List<FluidState>)`
— the block entity gathers `level.getFluidState(pos.relative(dir))` for all 6 directions and hands the list to the
helper, keeping the "is any neighbor water" predicate testable without a `Level` (`AdjacentWaterCheckTest`).
`isGenerating()` reflects `adjacentWater && generateAndMarkChanged(rate)`, so a full buffer correctly reports
not-generating even while water remains adjacent (`GeneratorGameTest#waterWheelDoesNotReportGeneratingWhenBufferFull`).

## Wind Turbine

Condition, in `WindTurbineBlockEntity.tick`: generates whenever it is both high enough (`pos.getY() >=
min_height`) **and** sufficiently exposed (at least `min_free_neighbors` of its 6 adjacent positions are air).
Both thresholds are `SsmConfig` values (`WIND_TURBINE_MIN_HEIGHT`, `WIND_TURBINE_MIN_FREE_NEIGHBORS`).

Unlike Solar Generator (which genuinely needs direct sunlight, and so correctly uses `level.canSeeSky`), Wind Turbine
originally reused `level.canSeeSky(pos.above())` too — a weather/light-visibility check, not a wind-relevant one,
since Minecraft has no actual wind simulation to check against. That was replaced with a height + open-neighbor
check that's thematically closer to "elevated and not boxed in," implemented as `WindExposureCheck.isExposed(int y,
int minHeight, List<BlockState> neighborStates, int minFreeNeighbors)` — a pure, unit-tested class
(`WindExposureCheckTest`) following the same pattern as `AdjacentWaterCheck`: the impure part (fetching each
neighbor's raw `BlockState`, which needs a live `Level`) stays in `WindTurbineBlockEntity#tick`, but the actual
predicate (`BlockState::isAir`) lives inside `WindExposureCheck` itself, not the caller — mirroring
`AdjacentWaterCheck` taking raw `FluidState`s and doing its own tag check, rather than the caller pre-computing a
`List<Boolean>` and handing over only the aggregate result (an earlier draft of this class did exactly that, and
was corrected once the parallel with `SkyColumnCheck`'s original flaw was pointed out). This superseded the earlier
`SkyColumnCheck` class (which, notably, was never actually wired into any block entity — it was dead code from the
start, extracted in anticipation of a multi-block-tall clearance check that never got built); `SkyColumnCheck`/
`SkyColumnCheckTest` were removed rather than left alongside the new class.

Like the Water Wheel, `isGenerating()` reflects `exposed && generateAndMarkChanged(rate)`, so a full buffer
correctly reports not-generating even when high enough and exposed
(`GeneratorGameTest#windTurbineDoesNotReportGeneratingWhenBufferFull`).

## Config-driven rates

Every generator's capacity, generation rate, and max output are `SsmConfig` values (see
[configuration.md](./configuration.md)) rather than hardcoded — the block entities read
`SsmConfig.*.get()` fresh each tick, so config changes take effect without needing a block entity reload.
