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
| **Wind Turbine**           | Generator  | Clear sky above                           | FE                                 | Generates whenever the block can see the sky                     |

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

The built jar is at `build/libs/ssm-<version>.jar`.

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

This is a **single-module** Gradle project — there is no `core`/`neoforge` split. `gametest/` is a separate Gradle
subproject only because NeoForge's ModDevGradle plugin requires one `neoForge {}` extension per project.

```
steves-simple-machines/
├── src/
│   ├── main/java/io/github/scuba10steve/ssm/
│   │   ├── Ssm.java              # @Mod entry point
│   │   ├── registry/             # DeferredRegister holders
│   │   ├── config/                # SsmConfig (ssm-common.toml)
│   │   ├── energy/                 # SimpleEnergyStorage
│   │   ├── machine/                # Generic processing-machine framework + MachineConfigs
│   │   ├── recipe/                 # CrushingRecipe, SawingRecipe
│   │   ├── generator/               # The 4 generators + shared GeneratorBlock
│   │   └── compat/jei/               # JEI plugin and recipe categories
│   ├── main/resources/             # neoforge.mods.toml, data/, assets/
│   └── test/java/                  # Unit tests
├── gametest/                        # NeoForge GameTest subproject
│   └── src/main/java/.../gametest/  # CrusherGameTest, SawmillGameTest, SmelterGameTest, GeneratorGameTest
├── buildSrc/                        # ssm.conventions / ssm.repositories convention plugins
├── docs/dev/                        # Developer documentation
├── build.gradle, settings.gradle, gradle.properties
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

## Links

- [GitHub Repository](https://github.com/scuba10steve/steves-simple-machines)
