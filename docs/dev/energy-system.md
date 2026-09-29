# Energy System

Steve's Simple Machines uses NeoForge's standard Forge Energy (FE) capability (`IEnergyStorage`) for all power
transfer. There is a single storage implementation shared by every machine and generator.

## `SimpleEnergyStorage`

```java
public class SimpleEnergyStorage extends EnergyStorage {
    public SimpleEnergyStorage(int capacity, int maxReceive, int maxExtract) { super(capacity, maxReceive, maxExtract); }
    public int generate(int amount) { ... }   // bypasses maxReceive
    public void setEnergy(int amount) { ... } // clamped setter, used on NBT load
}
```

It's a thin extension of NeoForge's own `EnergyStorage`, adding two things vanilla `EnergyStorage` doesn't give you:

- **`generate(int amount)`** — adds energy directly to the buffer, clamped to remaining capacity, and returns the
  amount actually accepted. This is how generators fill their own buffer. It deliberately bypasses `maxReceive`
  (which governs energy accepted *from other blocks* via `receiveEnergy`) — a generator's own production isn't an
  external transfer and shouldn't be capped by it.
- **`setEnergy(int amount)`** — a clamped setter used only when restoring the stored amount from NBT in
  `loadAdditional`; `EnergyStorage`'s own `energy` field has no public setter.

Every machine and generator constructs its own `SimpleEnergyStorage`:

- Machines (`ProcessingMachineBlockEntity`): `new SimpleEnergyStorage(config.energyCapacity().getAsInt(),
  Integer.MAX_VALUE, 0)` — unlimited receive (any neighbor can push FE in), zero extract (a machine is a pure
  sink and never sends energy back out).
- Generators: `new SimpleEnergyStorage(capacity, 0, maxOutput)` — zero receive (a generator never accepts FE from a
  neighbor; its buffer only fills via its own `generate(...)` call), and a configured `maxOutput` extract cap.

## Capability registration (`ModCapabilities`)

Every energy-capable `BlockEntityType` is registered against NeoForge's `Capabilities.EnergyStorage.BLOCK` in
`ModCapabilities#registerCapabilities`, listening on `RegisterCapabilitiesEvent`:

```java
event.registerBlockEntity(
        Capabilities.EnergyStorage.BLOCK,
        ModBlockEntities.CRUSHER.get(),
        (be, side) -> be.energyStorage);
```

This is repeated once per energy-capable block entity (Crusher, Sawmill, Smelter, Combustion Generator, Solar Panel,
Water Wheel, Wind Turbine) — each registration is a one-liner mapping a `BlockEntityType` to a `(blockEntity, side)
-> IEnergyStorage` accessor, always returning the block entity's own `energyStorage` field regardless of `side`
(none of these blocks have side-specific energy behavior, e.g. an input-only face). The three processing machines
also register `Capabilities.ItemHandler.BLOCK` the same way, pointing at their `inventory`; the Combustion Generator
registers its `fuelHandler` similarly.

**Adding a new energy-capable block entity always means adding one `registerBlockEntity(...)` call here** — the
capability system has no auto-discovery; a `BlockEntity` with a public `energyStorage` field is not automatically
exposed to hoppers/cables/other mods without this registration.

## Push-to-neighbor pattern (generators)

Machines are pure sinks — something else must push FE into them. All 4 generators push their own generated energy
out, on their own tick, rather than waiting to be pulled from. This loop, along with the FE buffer itself, the
NBT round-trip, and the lo/hi `ContainerData` bit-packing, lives once in the shared abstract base
`GeneratorBlockEntity` (`neoforge/src/main/java/io/github/scuba10steve/ssm/generator/GeneratorBlockEntity.java`) that
`CombustionGeneratorBlockEntity`, `SolarPanelBlockEntity`, `WaterWheelBlockEntity`, and `WindTurbineBlockEntity` all
extend. Each `tick(...)` static method ends by calling the inherited helper:

```java
protected final void pushEnergyToNeighbors(Level level, BlockPos pos, int maxOutput) {
    if (energyStorage.getEnergyStored() <= 0) {
        return;
    }
    for (Direction dir : Direction.values()) {
        IEnergyStorage neighbour = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
        if (neighbour != null && neighbour.canReceive()) {
            int toSend = Math.min(energyStorage.getEnergyStored(), maxOutput);
            int sent = neighbour.receiveEnergy(toSend, false);
            if (sent > 0) {
                energyStorage.extractEnergy(sent, false);
                setChanged();
            }
        }
    }
}
```

Notes on this pattern:

- It checks all 6 directions every tick and pushes to **every** neighbor that can receive, not just the first one —
  so a generator surrounded by multiple machines feeds all of them (up to its own buffer and each neighbor's
  `receiveEnergy` limits).
- `toSend` is capped by the generator's own `maxOutput` (its `SimpleEnergyStorage`'s `maxExtract`, read again here
  from config so it stays consistent with the storage's own construction) as well as by whatever is actually
  buffered — a generator never sends more than it has.
- `neighbour.canReceive()` is checked before attempting a transfer purely as a fast-path/clarity guard;
  `receiveEnergy` on a storage that can't receive would simply return 0 and be a no-op either way.
- `setChanged()` is only called when energy actually moved (`sent > 0`), avoiding redundant chunk-dirty marking on
  ticks where every neighbor's buffer happened to be full.

`GeneratorBlockEntity` also exposes `generateAndMarkChanged(int amount)`, which each generator's condition logic
must route through instead of calling `energyStorage.generate(...)` directly: it returns `true` only if energy was
actually accepted (not merely if the environmental condition was met), and callers drive their "am I generating"
state from that return value. This is what keeps a full-buffer generator from reporting itself as generating
forever (see `docs/dev/generators.md`) — a new generator gets this for free by extending the base class, rather
than needing to get the buffer-full edge case right on its own.
