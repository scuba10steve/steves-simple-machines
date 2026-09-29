# Configuration System

Steve's Simple Machines uses NeoForge's `ModConfigSpec` system to generate a TOML configuration file, written to
**`config/ssm-common.toml`** the first time the mod loads. It is registered as a **common** config
(`ModConfig.Type.COMMON`) via `modContainer.registerConfig(...)` in `Ssm`'s constructor.

## Configuration File Structure

`SsmConfig` (`io.github.scuba10steve.ssm.config.SsmConfig`) defines one section per machine and generator, each with
the same three-value shape: a capacity, a per-tick rate, and either an energy-per-tick cost (machines) or a max
output (generators).

### Machines

```toml
[crusher]
    capacity = 10000          # FE buffer capacity
    energy_per_tick = 40      # FE consumed per tick while processing
    process_ticks = 100       # Ticks required per operation

[smelter]
    capacity = 10000
    energy_per_tick = 40
    process_ticks = 100       # Flat; ignores vanilla smelting recipes' own cookingTime

[sawmill]
    capacity = 10000
    energy_per_tick = 40
    process_ticks = 80
```

### Generators

```toml
[combustion_generator]
    capacity = 50000           # FE buffer capacity
    generation_rate = 160      # FE generated per burn-tick
    max_output = 200           # Max FE pushed to a neighbor per tick

[solar_generator]
    capacity = 20000
    generation_rate = 20       # FE generated per tick during daytime with sky access
    max_output = 100

[water_wheel]
    capacity = 30000
    generation_rate = 30       # FE generated per tick while adjacent to water
    max_output = 120

[wind_turbine]
    capacity = 30000
    generation_rate = 30       # FE generated per tick while at/above min_height with min_free_neighbors open
    max_output = 120
    min_height = 100           # minimum Y-level required to generate
    min_free_neighbors = 5     # minimum number of the 6 adjacent positions that must be air
```

**Notes:**

- All `capacity` values must be `>= 1`; all rate/cost/output values must be `>= 0`.
- Every field is read live (`SsmConfig.X.get()`) on each tick by the relevant block entity — changing a value and
  reloading the config (or restarting) takes effect immediately, no block re-registration or chunk reload needed
  beyond however the config file itself is reloaded.
- There are currently no feature-toggle, recipe-difficulty, or integration-toggle sections (unlike
  `steves-simple-storage`'s config) — every section here is purely numeric tuning for the machine/generator it names.

## Implementation Details

### Config Registration

Registered in `Ssm.java`'s constructor: `modContainer.registerConfig(ModConfig.Type.COMMON, SsmConfig.SPEC,
"ssm-common.toml")`.

There is no platform-abstraction layer here (unlike `steves-simple-storage`'s `S3Config` interface) — `SsmConfig`
(`neoforge/src/main/java/io/github/scuba10steve/ssm/config/SsmConfig.java`) lives entirely in the `neoforge` module
alongside the block entities that use it, so they read its static fields directly without going through any
`core`-side abstraction (e.g. `SsmConfig.CRUSHER_ENERGY_PER_TICK.get()`).

### Adding a config value for a new machine/generator

Follow the existing per-section pattern in `SsmConfig`'s static initializer: `builder.comment("Name").push("id")`,
one or more `builder.comment(...).defineInRange(...)` calls, then `builder.pop()`. Add the corresponding
`ModConfigSpec.IntValue` fields alongside the existing ones, and wire them into the new `MachineConfig` entry (see
[machine-framework.md](./machine-framework.md)) or generator block entity (see
[generators.md](./generators.md)).

## Troubleshooting

### Config file not generated

1. Check `logs/latest.log` for errors during mod construction.
2. Ensure the `config/` directory exists and is writable.
3. Confirm the mod loaded successfully (`Steve's Simple Machines` appears in the mod list log line).

### Config changes not applied

1. Ensure the TOML file was saved before restarting/reloading.
2. Check for TOML syntax errors — an invalid file is reset to defaults and logged as
   "Configuration file is not correct."
3. Confirm values are within the ranges enforced by `defineInRange` (e.g. capacities `>= 1`).
