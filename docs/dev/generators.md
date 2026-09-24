# Generators

There are 4 generators, each with its own `BlockEntity`/`Menu`/`Screen` (unlike the 3 machines, which share one
implementation — generators differ enough in condition logic and per-tick behavior that a shared `GeneratorConfig`
wasn't worth the indirection). All 4 share `GeneratorBlock<T>` (block-level plumbing: `newBlockEntity`, `getTicker`,
opening the menu on right-click) and the push-to-neighbor energy pattern described in
[energy-system.md](./energy-system.md). What differs is each one's *condition logic* — what has to be
true, each tick, for it to generate.

## Combustion Generator

Burns solid fuel (coal, charcoal, coal blocks — the same hardcoded `getBurnTime` table as vanilla furnaces, not the
`FuelValues`/tag-driven vanilla fuel system) to generate FE at a fixed rate per burn-tick.

Condition logic, in `CombustionGeneratorBlockEntity.tick`:

1. If already burning, tick `CombustionBurnState` (see divergence below) and add any generated energy to the buffer.
2. If **not** burning and the buffer isn't already full, check the fuel slot; if it holds a valid fuel item, start
   burning (`burnState.startBurning(burnTime)`) and consume one fuel item.
3. Push to neighbors (shared pattern).

### Intentional divergence from `steves-advanced-storage`

This block entity is ported from `steves-advanced-storage`'s `CoalGeneratorBlockEntity`
(`neoforge/src/main/java/io/github/scuba10steve/s3/advanced/blockentity/CoalGeneratorBlockEntity.java`), but with one
deliberate behavioral change, isolated in `CombustionBurnState.tick(generationRate, bufferRoom)`:

```java
public TickResult tick(int generationRate, int bufferRoom) {
    if (remainingBurnTime <= 0) {
        return new TickResult(remainingBurnTime, 0);
    }
    if (bufferRoom <= 0) {
        return new TickResult(remainingBurnTime, 0); // paused: buffer full, don't consume burn time
    }
    remainingBurnTime--;
    return new TickResult(remainingBurnTime, generationRate);
}
```

- **`steves-advanced-storage`'s source behavior:** burn time counts down every tick regardless of whether the energy
  buffer has room, so a full buffer means the fuel item is silently wasted while it finishes burning with nothing to
  show for it.
- **`ssm`'s behavior:** when the buffer has no room (`bufferRoom <= 0`), the tick is a no-op — `remainingBurnTime` is
  **not** decremented and no energy is generated. Burning effectively pauses and resumes exactly where it left off
  once a neighbor drains the buffer, so a full output buffer never wastes fuel.

This is covered directly by `CombustionBurnStateTest#burnPausesWhenBufferIsFullAndResumesWithSpace`, which asserts a
tick with `bufferRoom = 0` leaves `remainingBurnTime` unchanged before asserting a following tick with room resumes
normally.

## Solar Panel

Condition, in `SolarPanelBlockEntity.tick`: generates only when `level.isDay()` **and** there is sky light directly
above the block (`level.getBrightness(LightLayer.SKY, pos.above()) > 0`) — a roof, or being underground, blocks
generation even during the day. `getCurrentRate()` (exposed via `ContainerData` for the screen) reflects the actual
rate only while both conditions hold; it's zeroed the instant either fails, including the edge case where
`generate(...)` accepted 0 because the buffer was already full.

## Water Wheel

Condition, in `WaterWheelBlockEntity.tick`: generates whenever any of the 6 orthogonally adjacent blocks is a water
source or flowing water. The check itself is the pure, unit-tested `AdjacentWaterCheck.hasAdjacentWater(List<FluidState>)`
— the block entity gathers `level.getFluidState(pos.relative(dir))` for all 6 directions and hands the list to the
helper, keeping the "is any neighbor water" predicate testable without a `Level` (`AdjacentWaterCheckTest`). Unlike
the Solar Panel and Combustion Generator, there's no buffer-related pause here: `generate(...)`'s own clamping to
`capacity - energy` already makes a full buffer a safe no-op.

## Wind Turbine

Condition, in `WindTurbineBlockEntity.tick`: generates whenever the column above the block is clear to the sky
(`level.canSeeSky(pos.above())`). Note that the block entity calls `Level#canSeeSky` **directly** rather than through
`SkyColumnCheck` — `SkyColumnCheck.isColumnClear(List<Boolean>)` exists as a pure, unit-tested helper for the general
"is every step in a column clear" predicate (`SkyColumnCheckTest`), but the actual runtime condition only needs a
single vanilla sky-visibility check at one position, so the block entity doesn't route through it. If a future
change needs a multi-block-tall clearance check (e.g. requiring N clear blocks above rather than just sky visibility
from the block itself), wire `WindTurbineBlockEntity` through `SkyColumnCheck` at that point.

## Config-driven rates

Every generator's capacity, generation rate, and max output are `SsmConfig` values (see
[configuration.md](./configuration.md)) rather than hardcoded — the block entities read
`SsmConfig.*.get()` fresh each tick, so config changes take effect without needing a block entity reload.
