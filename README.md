# Steve's Simple Machines (SSM)

Better power generation and automated ore/wood processing for Minecraft. Steve's Simple Machines adds three
FE-powered processing machines (Crusher, Sawmill, Smelter) and four generators (Combustion, Solar Panel, Water
Wheel, Wind Turbine) — a small, focused automation layer that plugs into NeoForge's standard Forge Energy (FE)
capability, so it interoperates with any other FE-based mod. Includes JEI integration for all machine recipes.

## User Documentation

### Getting Started

1. Craft a **Crusher**, **Sawmill**, or **Smelter** and place it down.
2. Feed it FE from a generator (see below) placed adjacent to it, or any other FE source.
3. Right-click the machine to open its GUI and insert raw materials into the input slot.

### Block Reference

| Block                    | Role       | Input                                   | Output                          | Notes                                                        |
|---------------------------|------------|-------------------------------------------|-----------------------------------|-----------------------------------------------------------------|
| **Crusher**                | Machine    | Raw ore (iron, gold, copper)              | 2x crushed ore                    | FE-powered; crushed ore smelts into ingots                     |
| **Smelter**                | Machine    | Any vanilla smelting input (ore, crushed ore, food, etc.) | Vanilla smelting result | Reuses vanilla `minecraft:smelting` recipes, powered by FE instead of fuel |
| **Sawmill**                | Machine    | Logs                                       | Planks + Sawdust                  | Two output slots; FE-powered                                    |
| **Combustion Generator**   | Generator  | Any vanilla or modded furnace fuel (coal, charcoal, sticks, etc.) | FE                                 | Burns fuel; pauses (doesn't waste fuel) when its buffer is full |
| **Solar Panel**            | Generator  | Daylight + clear sky above                | FE                                 | Generates only during the day with direct sky access             |
| **Water Wheel**            | Generator  | Adjacent water                            | FE                                 | Generates whenever any of the 6 neighbors is water                |
| **Wind Turbine**           | Generator  | High altitude + open surroundings         | FE                                 | Generates above a configurable min height with most sides unobstructed |

Generators automatically push their generated FE to any adjacent block that can receive it (including other mods'
FE-capable machines) — no cables or wires required for direct adjacency.

## Developer Documentation

See [AGENTS.md](AGENTS.md) for the full project map and conventions, and [docs/dev/](docs/dev/) for subsystem
documentation:

- [Machine Framework](docs/dev/machine-framework.md) — the generic processing-machine implementation shared by all
  3 machines
- [Energy System](docs/dev/energy-system.md) — `SimpleEnergyStorage`, capability registration, push-to-neighbor
- [Generators](docs/dev/generators.md) — each generator's condition logic
- [JEI Integration](docs/dev/jei-integration.md) — recipe categories and the catalyst-only pattern
- [Build System](docs/dev/build-system.md) — Gradle project layout and tasks
- [Configuration](docs/dev/configuration.md) — `config/ssm-common.toml` reference
- [Dependencies](docs/dev/dependencies.md) — dependency versions and update strategy
- [Troubleshooting](docs/dev/troubleshooting.md) — common build, test, and gameplay issues

### Project Status

Version 0.1.0 — in development, targeting **Minecraft 1.21.1** / **NeoForge 21.1.226**.

### Requirements

- **Java 21** or higher (required for NeoForge)
- **Gradle 9.x** (wrapper included)

### Building the Mod

```bash
git clone https://github.com/scuba10steve/steves-simple-machines.git
cd steves-simple-machines
./gradlew build
```

On Windows: `gradlew.bat build`

The built jar is at `neoforge/build/libs/ssm-<version>.jar`.

### Development

```bash
./gradlew runClient   # Run a development client
./gradlew runServer   # Run a development server
```

### Running Tests

```bash
# Unit tests
./gradlew test

# GameTests (separate Gradle subproject)
./gradlew :gametest:runGameTestServer

# Everything, matching CI
./gradlew build :gametest:runGameTestServer

# View the unit test report
open build/reports/tests/test/index.html
```

### Project Structure

This is a **two-module** Gradle project: `core` (platform-agnostic, vanilla MC classes only) and `neoforge` (the
NeoForge runtime module, depending on `core`). `gametest/` is a separate Gradle subproject depending on both, because
NeoForge's ModDevGradle plugin requires one `neoForge {}` extension per project.

```
steves-simple-machines/
├── core/                              # Platform-agnostic module (neoFormVersion only, no NeoForge APIs)
│   └── src/
│       ├── main/java/io/github/scuba10steve/ssm/
│       │   ├── ref/                   # RefStrings
│       │   ├── machine/               # MachineRecipeLookup, SsmMachineRecipe, SmelterAdapter, MachineInventory,
│       │   │                          #   ProcessingMachineState
│       │   └── generator/             # CombustionBurnState, AdjacentWaterCheck, WindExposureCheck
│       └── test/java/                 # 19 unit tests
├── neoforge/                          # NeoForge runtime module — everything else
│   └── src/
│       ├── main/java/io/github/scuba10steve/ssm/
│       │   ├── Ssm.java               # @Mod entry point
│       │   ├── registry/              # DeferredRegister holders
│       │   ├── config/                # SsmConfig (ssm-common.toml)
│       │   ├── energy/                # SimpleEnergyStorage
│       │   ├── machine/               # ProcessingMachineBlock/BlockEntity/Menu/Screen, MachineConfigs,
│       │   │                          #   ItemStackHandlerInventory
│       │   ├── recipe/                # CrushingRecipe, SawingRecipe
│       │   ├── generator/             # The 4 generators + shared GeneratorBlock/GeneratorBlockEntity
│       │   └── compat/jei/            # JEI plugin and recipe categories
│       ├── main/resources/            # neoforge.mods.toml, data/, assets/
│       └── test/java/                 # 9 unit tests
├── gametest/                          # NeoForge GameTest subproject (depends on :core and :neoforge)
│   └── src/main/java/.../gametest/    # CrusherGameTest, SawmillGameTest, SmelterGameTest, GeneratorGameTest,
│                                       #   BlockDropGameTest (24 gametests total)
├── buildSrc/                          # ssm.conventions / ssm.neoforge-mod / ssm.repositories convention plugins
├── docs/dev/                          # Developer documentation
├── settings.gradle, gradle.properties # Root project is inert — no root build.gradle
└── AGENTS.md, README.md, CHANGELOG.md, cliff.toml
```

### Configuration

Steve's Simple Machines writes a TOML config to `config/ssm-common.toml` on first launch, with one section per
machine/generator controlling FE buffer capacity, generation/consumption rate, and (for generators) max output per
tick. See [docs/dev/configuration.md](docs/dev/configuration.md) for the full reference. Config changes require a
game restart (or config reload) to take effect.

## License

Licensed under the MIT License (`mod_license` in `gradle.properties`).

## Credits

- **Author**: scuba10steve
- **Game**: [Minecraft](http://www.minecraft.net/) by [Mojang AB](http://mojang.com/)
- **Mod Loader**: [NeoForge](https://neoforged.net/)
- Combustion Generator and Solar Panel are adapted from `steves-advanced-storage`'s generator block entities.
- Block textures for **Smelter** (`device_smelter_bottom/front/side/top`) are sourced from [malcolmriley/unused-textures](https://github.com/malcolmriley/unused-textures) via a [personal fork](https://github.com/scuba10steve/unused-minecraft-textures), used under the [Creative Commons Attribution 4.0 International License (CC-BY-4.0)](https://creativecommons.org/licenses/by/4.0/).
- Block textures for **Crusher** (`block_breaker_side/top`) and **Sawmill** (`bronze_sawmill_bottom/front_off/side/top`) are sourced from [Futureazoo/TextureRepository](https://github.com/Futureazoo/TextureRepository), used under the [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License (CC BY-NC-SA 4.0)](https://creativecommons.org/licenses/by-nc-sa/4.0/).

## Links

- [GitHub Repository](https://github.com/scuba10steve/steves-simple-machines)
