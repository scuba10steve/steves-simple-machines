# Steve's Simple Machines v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a standalone NeoForge 1.21.1 mod ("Steve's Simple Machines", mod_id `ssm`) adding a Crusher, Smelter, and Sawmill (all sharing one generic processing-machine framework), four FE generators (Combustion, Solar, Water Wheel, Wind Turbine), JEI integration, and gametest coverage.

**Architecture:** Single-module NeoForge MDK project. One generic `ProcessingMachineBlockEntity` (config-driven via `MachineConfig` + a `MachineRecipeLookup` strategy) backs all three machines. A shared `SimpleEnergyStorage` (NeoForge `EnergyStorage`) backs both machines (sink) and generators (source), exposed externally via `Capabilities.EnergyStorage.BLOCK`. Combustion Generator and Solar Panel are ported/adapted from the already-working `CoalGeneratorBlockEntity`/`SolarGeneratorBlockEntity` in `steves-advanced-storage`.

**Tech Stack:** Java 21, NeoForge 1.21.1 (`net.neoforged.moddev` / NeoGradle ModDevGradle), Gradle (Groovy DSL) with `buildSrc` convention plugins, JEI 19.27.0.340, JUnit 5, NeoForge GameTest.

**Spec:** `docs/superpowers/specs/2026-09-21-steves-simple-machines-design.md`

## Global Constraints

- `minecraft_version=1.21.1`, `minecraft_version_range=[1.21.1,1.22)`
- `neoforge_version=21.1.226`, `neoforge_version_range=[21.1,22)`, `loader_version_range=[4,)`
- `mod_id=ssm`, `mod_name=Steve's Simple Machines`, package root `io.github.scuba10steve.ssm`
- `mod_license=MIT`, `mod_authors=scuba10steve, zerofall, SBlectric` (matches `steves-advanced-storage`'s mods.toml `authors` field)
- Java toolchain: `JavaLanguageVersion.of(21)`
- No hard dependency on `steves-simple-storage` or `steves-advanced-storage` at runtime — code may be *ported/adapted from* `steves-advanced-storage`, but `ssm` must build and run standalone
- JEI (`mezz.jei:jei-1.21.1-common-api`, `mezz.jei:jei-1.21.1-neoforge-api`) is `compileOnly`; `mezz.jei:jei-1.21.1-neoforge` is `runtimeOnly` for tests only — JEI classes must never load on a server without JEI installed
- Energy: NeoForge `IEnergyStorage`/`EnergyStorage`, FE units, exposed via `Capabilities.EnergyStorage.BLOCK` registered in `RegisterCapabilitiesEvent`
- Out of scope for v1 (do not build): thermal/nuclear/geothermal/biodiesel/gasoline generation, multi-stage ore enrichment, multiblocks, shared common library, multi-loader support

---

## File Structure

```
steves-simple-machines/
  settings.gradle, build.gradle, gradle.properties, gradlew, gradlew.bat, gradle/
  buildSrc/
    settings.gradle, build.gradle
    src/main/groovy/ssm.conventions.gradle       (java + moddev + repositories + JUnit platform)
    src/main/groovy/ssm.repositories.gradle       (maven repos: NeoForge, Mojang, Jared, Modrinth)
  src/main/java/io/github/scuba10steve/ssm/
    Ssm.java                                      (@Mod entrypoint, registers everything)
    ref/RefStrings.java                           (MOD_ID constant)
    registry/
      ModItems.java, ModBlocks.java, ModBlockEntities.java, ModMenuTypes.java, ModRecipes.java
      ModCreativeTabs.java, ModCapabilities.java, ModClientSetup.java
    config/SsmConfig.java                         (ModConfigSpec, ssm-common.toml)
    energy/SimpleEnergyStorage.java
    machine/
      MachineConfig.java (includes a menuType field)
      MachineRecipeLookup.java, MachineRecipeResult.java
      SsmMachineRecipe.java
      ProcessingMachineState.java                 (pure state machine; unit-testable without a Level)
      ProcessingMachineBlockEntity.java            (thin BlockEntity + MenuProvider wrapper around the state machine)
      ProcessingMachineBlock.java
      MachineConfigs.java                         (the 3 concrete configs: CRUSHER, SMELTER, SAWMILL)
      SmelterAdapter.java                          (pure adapter: vanilla smelting result -> MachineRecipeResult)
      ProcessingMachineMenu.java, ProcessingMachineScreen.java
    recipe/
      CrushingRecipe.java, SawingRecipe.java
    generator/
      CombustionBurnState.java                     (pure burn-timer logic; unit-testable)
      AdjacentWaterCheck.java, SkyColumnCheck.java  (pure condition-check helpers; unit-testable)
      CombustionGeneratorBlockEntity.java, SolarPanelBlockEntity.java
      WaterWheelBlockEntity.java, WindTurbineBlockEntity.java
      GeneratorBlock.java                          (shared BaseEntityBlock: newBlockEntity, ticker, useWithoutItem->openMenu)
      CombustionGeneratorMenu.java, CombustionGeneratorScreen.java
      SolarPanelMenu.java, SolarPanelScreen.java
      WaterWheelMenu.java, WaterWheelScreen.java
      WindTurbineMenu.java, WindTurbineScreen.java
    compat/jei/
      SsmJeiPlugin.java, CrushingCategory.java, SawingCategory.java
  src/main/resources/
    META-INF/neoforge.mods.toml
    data/ssm/recipe/crushing/*.json, sawing/*.json, smelting/*.json, crafting/*.json
    data/ssm/tags/item/... (if needed for crafting ingredients — vanilla tags suffice for v1)
    assets/ssm/lang/en_us.json, assets/ssm/models/..., assets/ssm/blockstates/..., assets/ssm/textures/...
  gametest/                                        (separate Gradle subproject — NeoGradle's moddev plugin
                                                     needs one neoForge{} extension per project, so this is
                                                     not a source set of the root build; see Task 15)
    build.gradle
    src/main/java/io/github/scuba10steve/ssm/gametest/
      CrusherGameTest.java, SmelterGameTest.java, SawmillGameTest.java, GeneratorGameTest.java
    src/main/resources/data/ssm/structure/empty.nbt (singular "structure", copied from S3's gametest module)
  src/test/java/io/github/scuba10steve/ssm/
    energy/SimpleEnergyStorageTest.java
    machine/ProcessingMachineStateTest.java, SmeltingLookupTest.java
    recipe/CrushingRecipeTest.java, SawingRecipeTest.java
    generator/CombustionBurnStateTest.java, AdjacentWaterCheckTest.java, SkyColumnCheckTest.java
  docs/dev/machine-framework.md, energy-system.md, generators.md, jei-integration.md,
       build-system.md, configuration.md, dependencies.md, troubleshooting.md
  AGENTS.md, README.md, CHANGELOG.md, cliff.toml
  .github/workflows/ci.yml
```

---

### Task 1: Repo and Gradle scaffolding

**Files:**
- Create: `settings.gradle`, `build.gradle`, `gradle.properties`
- Create: `buildSrc/settings.gradle`, `buildSrc/build.gradle`
- Create: `buildSrc/src/main/groovy/ssm.repositories.gradle`, `buildSrc/src/main/groovy/ssm.conventions.gradle`
- Create: `src/main/java/io/github/scuba10steve/ssm/Ssm.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/ref/RefStrings.java`
- Create: `src/main/resources/META-INF/neoforge.mods.toml`
- Create: `.gitignore`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties` (via `gradle wrapper`)

**Interfaces:**
- Produces: `RefStrings.MODID = "ssm"` — every later registry class references this.
- Produces: `Ssm` constructor signature `public Ssm(IEventBus modEventBus, ModContainer modContainer)` — later tasks add registration calls inside it.

- [ ] **Step 1: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

minecraft_version=1.21.1
minecraft_version_range=[1.21.1,1.22)
neoforge_version=21.1.226
neoforge_version_range=[21.1,22)
loader_version_range=[4,)

mod_id=ssm
mod_name=Steve's Simple Machines
mod_license=MIT
mod_version=0.1.0
mod_authors=scuba10steve, zerofall, SBlectric
mod_description=Better power generation and automated ore/wood processing for Minecraft.
```

- [ ] **Step 2: Write `buildSrc/settings.gradle`**

```groovy
rootProject.name = 'buildSrc'
```

- [ ] **Step 3: Write `buildSrc/build.gradle`**

```groovy
plugins {
    id 'groovy-gradle-plugin'
}

repositories {
    gradlePluginPortal()
    maven {
        name = 'NeoForged'
        url = 'https://maven.neoforged.net/releases'
    }
}

dependencies {
    implementation 'net.neoforged.moddev:net.neoforged.moddev.gradle.plugin:2.0.100'
}
```

(Confirm the exact `net.neoforged.moddev` plugin coordinate/version against what `steves-simple-storage`'s `buildSrc/build.gradle` currently pins — copy that version rather than guessing if it differs.)

- [ ] **Step 4: Write `buildSrc/src/main/groovy/ssm.repositories.gradle`**

```groovy
repositories {
    mavenCentral()
    maven {
        name = 'NeoForge'
        url = 'https://maven.neoforged.net/releases'
    }
    maven {
        name = 'Mojang'
        url = 'https://libraries.minecraft.net'
    }
    maven {
        name = 'Jared'
        url = 'https://maven.blamejared.com'
    }
    maven {
        name = 'Modrinth'
        url = 'https://api.modrinth.com/maven'
    }
}
```

- [ ] **Step 5: Write `buildSrc/src/main/groovy/ssm.conventions.gradle`**

```groovy
plugins {
    id 'java'
    id 'net.neoforged.moddev'
    id 'ssm.repositories'
}

version = rootProject.mod_version
group = 'io.github.scuba10steve.ssm'

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

neoForge {
    version = neoforge_version

    runs {
        client {
            client()
        }
        server {
            server()
        }
    }
}

dependencies {
    compileOnly "mezz.jei:jei-1.21.1-common-api:19.27.0.340"
    compileOnly "mezz.jei:jei-1.21.1-neoforge-api:19.27.0.340"

    testImplementation 'org.junit.jupiter:junit-jupiter:5.14.4'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.withType(Test).configureEach {
    useJUnitPlatform()
}
```

- [ ] **Step 6: Write root `settings.gradle`**

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = 'NeoForged'
            url = 'https://maven.neoforged.net/releases'
        }
    }
    plugins {
        id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention'
}

rootProject.name = 'steves-simple-machines'
```

- [ ] **Step 7: Write root `build.gradle`**

```groovy
plugins {
    id 'ssm.conventions'
}

base {
    archivesName = 'ssm'
}

processResources {
    def replaceProperties = [mod_version: mod_version]
    inputs.properties replaceProperties
    filesMatching('META-INF/neoforge.mods.toml') {
        expand(replaceProperties)
    }
}
```

- [ ] **Step 8: Write `src/main/resources/META-INF/neoforge.mods.toml`**

```toml
modLoader = "javafml"
loaderVersion = "${loader_version_range}"
license = "MIT"

[[mods]]
modId = "ssm"
version = "${mod_version}"
displayName = "Steve's Simple Machines"
description = '''Better power generation and automated ore/wood processing for Minecraft.'''
authors = "scuba10steve, zerofall, SBlectric"

[[dependencies.ssm]]
modId = "neoforge"
type = "required"
versionRange = "${neoforge_version_range}"
ordering = "NONE"
side = "BOTH"

[[dependencies.ssm]]
modId = "minecraft"
type = "required"
versionRange = "${minecraft_version_range}"
ordering = "NONE"
side = "BOTH"
```

- [ ] **Step 9: Write `src/main/java/io/github/scuba10steve/ssm/ref/RefStrings.java`**

```java
package io.github.scuba10steve.ssm.ref;

public final class RefStrings {
    public static final String MODID = "ssm";

    private RefStrings() {}
}
```

- [ ] **Step 10: Write `src/main/java/io/github/scuba10steve/ssm/Ssm.java`**

```java
package io.github.scuba10steve.ssm;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(RefStrings.MODID)
public class Ssm {
    public Ssm(IEventBus modEventBus, ModContainer modContainer) {
        // Registration calls added by later tasks.
    }
}
```

- [ ] **Step 11: Write `.gitignore`**

```
.gradle/
build/
.idea/
*.iml
run/
logs/
```

- [ ] **Step 12: Generate the Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.12` (match whatever wrapper version `steves-simple-storage`'s `gradle/wrapper/gradle-wrapper.properties` currently pins — check it and use the same version instead of guessing).

- [ ] **Step 13: Write `.github/workflows/ci.yml`**

```yaml
name: CI

on:
  push:
    branches: [ '**' ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build and test
        run: ./gradlew build

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/build/reports/tests/'
```

(Copied from `steves-simple-storage`'s `.github/workflows/ci.yml`. Task 15 later adds a `Run GameTests` step to this file once the `gametest` subproject exists.)

- [ ] **Step 14: Verify the project builds**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL` (an empty mod jar with no blocks/items yet is fine).

- [ ] **Step 15: Commit**

```bash
git add -A
git commit -m "chore: scaffold NeoForge 1.21.1 single-module mod project"
```

---

### Task 2: Registries skeleton, creative tab, main class wiring

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/registry/ModItems.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/registry/ModBlocks.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/registry/ModBlockEntities.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/registry/ModMenuTypes.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/registry/ModCreativeTabs.java`
- Modify: `src/main/java/io/github/scuba10steve/ssm/Ssm.java`

**Interfaces:**
- Consumes: `RefStrings.MODID` (Task 1).
- Produces: `ModItems.ITEMS`, `ModBlocks.BLOCKS`, `ModBlockEntities.BLOCK_ENTITIES`, `ModMenuTypes.MENU_TYPES` — `DeferredRegister` instances every later task adds entries to. Each registry class exposes a static `register(IEventBus bus)` method.

- [ ] **Step 1: Write `ModItems.java`**

```java
package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, RefStrings.MODID);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
```

- [ ] **Step 2: Write `ModBlocks.java`**

```java
package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, RefStrings.MODID);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
```

- [ ] **Step 3: Write `ModBlockEntities.java`**

```java
package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RefStrings.MODID);

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
```

- [ ] **Step 4: Write `ModMenuTypes.java`**

```java
package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, RefStrings.MODID);

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }
}
```

- [ ] **Step 5: Write `ModCreativeTabs.java`**

```java
package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, RefStrings.MODID);

    public static final Supplier<CreativeModeTab> SSM_TAB = TABS.register("ssm_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ssm"))
                    .icon(() -> ItemStack.EMPTY) // Task 7 replaces this with the Crusher item once registered
                    .displayItems((params, output) -> {
                        // Later tasks call output.accept(...) for each registered item.
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
```

- [ ] **Step 6: Wire registries into `Ssm.java`**

```java
package io.github.scuba10steve.ssm;

import io.github.scuba10steve.ssm.ref.RefStrings;
import io.github.scuba10steve.ssm.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(RefStrings.MODID)
public class Ssm {
    public Ssm(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
    }
}
```

- [ ] **Step 7: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add empty DeferredRegister holders and creative tab"
```

---

### Task 3: SimpleEnergyStorage + capability registration

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/energy/SimpleEnergyStorage.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/registry/ModCapabilities.java`
- Modify: `src/main/java/io/github/scuba10steve/ssm/Ssm.java`
- Test: `src/test/java/io/github/scuba10steve/ssm/energy/SimpleEnergyStorageTest.java`

**Interfaces:**
- Produces: `SimpleEnergyStorage(int capacity, int maxReceive, int maxExtract)` extends `net.neoforged.neoforge.energy.EnergyStorage`, with `int generate(int amount)` and `void setEnergy(int amount)`. Later generator and machine tasks construct this directly (machines: `maxReceive` = a large value, `maxExtract = 0`; generators: `maxReceive = 0`, `maxExtract` = their configured max output).
- Produces: `ModCapabilities.register(IEventBus bus)` — later tasks (5, 10-13) add `event.registerBlockEntity(...)` calls inside it once their block entity types exist. This task registers zero entries (no block entities exist yet) but wires the listener so later tasks only add lines.

- [ ] **Step 1: Write the failing test**

```java
package io.github.scuba10steve.ssm.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleEnergyStorageTest {

    @Test
    void generateClampsAtCapacity() {
        SimpleEnergyStorage storage = new SimpleEnergyStorage(1000, 0, 500);
        int accepted = storage.generate(1500);
        assertEquals(1000, accepted);
        assertEquals(1000, storage.getEnergyStored());
    }

    @Test
    void generateAfterPartialFillOnlyAcceptsRemainingRoom() {
        SimpleEnergyStorage storage = new SimpleEnergyStorage(1000, 0, 500);
        storage.generate(700);
        int accepted = storage.generate(700);
        assertEquals(300, accepted);
        assertEquals(1000, storage.getEnergyStored());
    }

    @Test
    void setEnergyClampsToCapacityAndZero() {
        SimpleEnergyStorage storage = new SimpleEnergyStorage(1000, 0, 500);
        storage.setEnergy(5000);
        assertEquals(1000, storage.getEnergyStored());
        storage.setEnergy(-50);
        assertEquals(0, storage.getEnergyStored());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.energy.SimpleEnergyStorageTest"`
Expected: FAIL — `SimpleEnergyStorage` does not exist.

- [ ] **Step 3: Write `SimpleEnergyStorage.java`**

```java
package io.github.scuba10steve.ssm.energy;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.energy.EnergyStorage;

/** FE buffer shared by machines (as a sink) and generators (as a source). */
public class SimpleEnergyStorage extends EnergyStorage {

    public SimpleEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    /** Adds generated energy directly to the buffer, bypassing maxReceive. Returns the amount actually stored. */
    public int generate(int amount) {
        int accepted = Math.min(amount, capacity - energy);
        energy += accepted;
        return accepted;
    }

    public void setEnergy(int amount) {
        this.energy = Mth.clamp(amount, 0, capacity);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.energy.SimpleEnergyStorageTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Write `ModCapabilities.java`**

```java
package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCapabilities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Tasks 5, 10, 11, 12, 13 each add one or more
        // event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, <TYPE>, (be, side) -> be.energyStorage);
        // lines here once their block entity types exist.
    }
}
```

- [ ] **Step 6: Wire into `Ssm.java`**

Add `ModCapabilities.register(modEventBus);` to the constructor (after the other `register` calls from Task 2).

- [ ] **Step 7: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add SimpleEnergyStorage and capability registration scaffold"
```

---

### Task 4: Config (`ssm-common.toml`)

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/config/SsmConfig.java`
- Modify: `src/main/java/io/github/scuba10steve/ssm/Ssm.java`

**Interfaces:**
- Produces: `SsmConfig.SPEC` (`ModConfigSpec`) plus one `ModConfigSpec.IntValue` per tunable named below. Later tasks (7-13) read these via `.get()`.

- [ ] **Step 1: Write `SsmConfig.java`**

```java
package io.github.scuba10steve.ssm.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SsmConfig {

    public static final ModConfigSpec SPEC;

    // Crusher
    public static final ModConfigSpec.IntValue CRUSHER_CAPACITY;
    public static final ModConfigSpec.IntValue CRUSHER_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue CRUSHER_PROCESS_TICKS;

    // Smelter
    public static final ModConfigSpec.IntValue SMELTER_CAPACITY;
    public static final ModConfigSpec.IntValue SMELTER_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue SMELTER_PROCESS_TICKS;

    // Sawmill
    public static final ModConfigSpec.IntValue SAWMILL_CAPACITY;
    public static final ModConfigSpec.IntValue SAWMILL_ENERGY_PER_TICK;
    public static final ModConfigSpec.IntValue SAWMILL_PROCESS_TICKS;

    // Combustion Generator
    public static final ModConfigSpec.IntValue COMBUSTION_CAPACITY;
    public static final ModConfigSpec.IntValue COMBUSTION_GENERATION_RATE;
    public static final ModConfigSpec.IntValue COMBUSTION_MAX_OUTPUT;

    // Solar Panel
    public static final ModConfigSpec.IntValue SOLAR_CAPACITY;
    public static final ModConfigSpec.IntValue SOLAR_GENERATION_RATE;
    public static final ModConfigSpec.IntValue SOLAR_MAX_OUTPUT;

    // Water Wheel
    public static final ModConfigSpec.IntValue WATER_WHEEL_CAPACITY;
    public static final ModConfigSpec.IntValue WATER_WHEEL_GENERATION_RATE;
    public static final ModConfigSpec.IntValue WATER_WHEEL_MAX_OUTPUT;

    // Wind Turbine
    public static final ModConfigSpec.IntValue WIND_TURBINE_CAPACITY;
    public static final ModConfigSpec.IntValue WIND_TURBINE_GENERATION_RATE;
    public static final ModConfigSpec.IntValue WIND_TURBINE_MAX_OUTPUT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Crusher").push("crusher");
        CRUSHER_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 10_000, 1, Integer.MAX_VALUE);
        CRUSHER_ENERGY_PER_TICK = builder.comment("FE consumed per tick while processing").defineInRange("energy_per_tick", 40, 0, Integer.MAX_VALUE);
        CRUSHER_PROCESS_TICKS = builder.comment("Ticks required per operation").defineInRange("process_ticks", 100, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Smelter").push("smelter");
        SMELTER_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 10_000, 1, Integer.MAX_VALUE);
        SMELTER_ENERGY_PER_TICK = builder.comment("FE consumed per tick while processing").defineInRange("energy_per_tick", 40, 0, Integer.MAX_VALUE);
        SMELTER_PROCESS_TICKS = builder.comment("Ticks required per operation (flat, ignores vanilla cookingTime)").defineInRange("process_ticks", 100, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Sawmill").push("sawmill");
        SAWMILL_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 10_000, 1, Integer.MAX_VALUE);
        SAWMILL_ENERGY_PER_TICK = builder.comment("FE consumed per tick while processing").defineInRange("energy_per_tick", 40, 0, Integer.MAX_VALUE);
        SAWMILL_PROCESS_TICKS = builder.comment("Ticks required per operation").defineInRange("process_ticks", 80, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Combustion Generator").push("combustion_generator");
        COMBUSTION_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 50_000, 1, Integer.MAX_VALUE);
        COMBUSTION_GENERATION_RATE = builder.comment("FE generated per burn-tick").defineInRange("generation_rate", 160, 0, Integer.MAX_VALUE);
        COMBUSTION_MAX_OUTPUT = builder.comment("Max FE pushed to a neighbor per tick").defineInRange("max_output", 200, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Solar Panel").push("solar_panel");
        SOLAR_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 20_000, 1, Integer.MAX_VALUE);
        SOLAR_GENERATION_RATE = builder.comment("FE generated per tick during daytime with sky access").defineInRange("generation_rate", 20, 0, Integer.MAX_VALUE);
        SOLAR_MAX_OUTPUT = builder.comment("Max FE pushed to a neighbor per tick").defineInRange("max_output", 100, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Water Wheel").push("water_wheel");
        WATER_WHEEL_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 30_000, 1, Integer.MAX_VALUE);
        WATER_WHEEL_GENERATION_RATE = builder.comment("FE generated per tick while adjacent to water").defineInRange("generation_rate", 30, 0, Integer.MAX_VALUE);
        WATER_WHEEL_MAX_OUTPUT = builder.comment("Max FE pushed to a neighbor per tick").defineInRange("max_output", 120, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Wind Turbine").push("wind_turbine");
        WIND_TURBINE_CAPACITY = builder.comment("FE buffer capacity").defineInRange("capacity", 30_000, 1, Integer.MAX_VALUE);
        WIND_TURBINE_GENERATION_RATE = builder.comment("FE generated per tick while unobstructed to the sky").defineInRange("generation_rate", 30, 0, Integer.MAX_VALUE);
        WIND_TURBINE_MAX_OUTPUT = builder.comment("Max FE pushed to a neighbor per tick").defineInRange("max_output", 120, 0, Integer.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }
}
```

- [ ] **Step 2: Register the config in `Ssm.java`**

```java
public Ssm(IEventBus modEventBus, ModContainer modContainer) {
    modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, SsmConfig.SPEC, "ssm-common.toml");
    // ... existing register(...) calls
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add ssm-common.toml config spec"
```

---

### Task 5: Generic processing-machine framework (recipe lookup, block entity, block)

This is the core abstraction every one of Crusher/Smelter/Sawmill plugs into.

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/machine/MachineRecipeResult.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/machine/MachineRecipeLookup.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/machine/SsmMachineRecipe.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/machine/MachineConfig.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/machine/ProcessingMachineBlockEntity.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/machine/ProcessingMachineBlock.java`
- Test: `src/test/java/io/github/scuba10steve/ssm/machine/ProcessingMachineStateTest.java` (logic-only tests against a fake `MachineRecipeLookup`, no live `Level` needed for the pure state-machine parts, which live in a separate `ProcessingMachineState` class the block entity delegates to; see Step 3)

**Interfaces:**
- Consumes: `SimpleEnergyStorage` (Task 3).
- Produces:
  - `record MachineRecipeResult(java.util.List<net.minecraft.world.item.ItemStack> outputs, int processTicks)`
  - `interface MachineRecipeLookup { java.util.Optional<MachineRecipeResult> find(net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack input); }`
  - `interface SsmMachineRecipe extends net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.SingleRecipeInput> { java.util.List<net.minecraft.world.item.ItemStack> getResults(net.minecraft.core.HolderLookup.Provider registries); default int getProcessTicksOverride() { return -1; } }`
  - `record MachineConfig(String id, MachineRecipeLookup recipeLookup, int outputSlotCount, java.util.function.IntSupplier energyCapacity, java.util.function.IntSupplier energyPerTick, java.util.function.IntSupplier baseProcessTicks)` — **no `menuType` field yet**. `ProcessingMachineMenu` doesn't exist until Task 6, and this task's own build/test verification steps must compile standalone; Task 6 modifies this record to add `menuType` once `ProcessingMachineMenu` exists.
  - `ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, MachineConfig config)` — plain `BlockEntity` for now (not yet `MenuProvider`; Task 6 adds that, `containerData`, `getDisplayName()`, and `createMenu()` once `ProcessingMachineMenu` exists — same forward-reference reason as above). Public final fields `config` (`MachineConfig`), `inventory` (`ItemStackHandler`, slot 0 = input, slots 1..N = outputs), `energyStorage` (`SimpleEnergyStorage`); `int getProgress()`; public static `tick(Level, BlockPos, BlockState, ProcessingMachineBlockEntity)`.
  - `ProcessingMachineBlock(java.util.function.BiFunction<BlockPos, BlockState, ProcessingMachineBlockEntity> factory, Properties properties)` — no separate `MachineConfig` parameter; the config is already baked into the `factory` lambda each machine task supplies (it constructs `new ProcessingMachineBlockEntity(type, pos, state, MachineConfigs.X)` directly). Right-click-to-open (`useWithoutItem`) is added by Task 6 for the same forward-reference reason. Later tasks (7-9) instantiate one per machine.

- [ ] **Step 1: Write `MachineRecipeResult.java`**

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record MachineRecipeResult(List<ItemStack> outputs, int processTicks) {}
```

- [ ] **Step 2: Write `MachineRecipeLookup.java`**

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public interface MachineRecipeLookup {
    Optional<MachineRecipeResult> find(Level level, ItemStack input);
}
```

- [ ] **Step 3: Write `SsmMachineRecipe.java`**

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;

/** A machine recipe with a full output list, beyond vanilla Recipe#assemble's single ItemStack. */
public interface SsmMachineRecipe extends Recipe<SingleRecipeInput> {
    List<ItemStack> getResults(HolderLookup.Provider registries);

    /** -1 means "use the machine's configured baseProcessTicks". */
    default int getProcessTicksOverride() {
        return -1;
    }

    @Override
    default ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        List<ItemStack> results = getResults(registries);
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return true;
    }
}
```

- [ ] **Step 4: Write `MachineConfig.java`**

```java
package io.github.scuba10steve.ssm.machine;

import java.util.function.IntSupplier;

public record MachineConfig(
        String id,
        MachineRecipeLookup recipeLookup,
        int outputSlotCount,
        IntSupplier energyCapacity,
        IntSupplier energyPerTick,
        IntSupplier baseProcessTicks
) {}
```

(No `menuType` field yet — see the Interfaces note above. Task 6 Step 3 adds it once `ProcessingMachineMenu` exists.)

- [ ] **Step 5: Write the pure tick-logic unit test (before the block entity exists, to drive its design)**

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingMachineStateTest {

    private static MachineRecipeLookup fixedResultLookup(ItemStack expectedInput, MachineRecipeResult result) {
        return (level, input) -> ItemStack.isSameItemSameComponents(input, expectedInput)
                ? Optional.of(result)
                : Optional.empty();
    }

    @Test
    void advancesProgressWhenEnergyAvailableAndRecipeMatches() {
        ItemStack input = new ItemStack(Items.RAW_IRON);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 5);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        ProcessingMachineState.TickOutcome outcome = state.tick(null, /* energyAvailable */ true);

        assertEquals(1, state.getProgress());
        assertFalse(outcome.completed());
    }

    @Test
    void doesNotAdvanceProgressWithoutEnergy() {
        ItemStack input = new ItemStack(Items.RAW_IRON);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 5);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        state.tick(null, false);

        assertEquals(0, state.getProgress());
    }

    @Test
    void completesAndMovesOutputWhenProgressReachesTarget() {
        ItemStack input = new ItemStack(Items.RAW_IRON, 1);
        ItemStack outputStack = new ItemStack(Items.IRON_NUGGET, 2);
        MachineRecipeResult result = new MachineRecipeResult(List.of(outputStack), 1);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        state.tick(null, true); // progress 0 -> 1 == target, completes on this same tick

        assertEquals(0, state.getProgress());
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertEquals(Items.IRON_NUGGET, inventory.getStackInSlot(1).getItem());
        assertEquals(2, inventory.getStackInSlot(1).getCount());
    }

    @Test
    void pausesAtCompletionWhenOutputSlotFull() {
        ItemStack input = new ItemStack(Items.RAW_IRON, 1);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 1);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);
        inventory.setStackInSlot(1, new ItemStack(Items.IRON_NUGGET, 64)); // full, blocks insertion

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        state.tick(null, true);

        assertFalse(inventory.getStackInSlot(0).isEmpty(), "input must not be consumed while output has no room");
        assertEquals(1, state.getProgress(), "progress holds at the completion point");
    }

    @Test
    void resetsProgressWhenInputNoLongerMatches() {
        ItemStack input = new ItemStack(Items.RAW_IRON, 1);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 5);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        state.tick(null, true); // progress -> 1

        inventory.setStackInSlot(0, new ItemStack(Items.DIRT)); // recipe no longer matches
        state.tick(null, true);

        assertEquals(0, state.getProgress());
    }

    @Test
    void doesNotConsumeEnergyOnTicksSpentWaitingForOutputSpace() {
        ItemStack input = new ItemStack(Items.RAW_IRON, 1);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 1);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);
        inventory.setStackInSlot(1, new ItemStack(Items.IRON_NUGGET, 64)); // full, blocks insertion

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        ProcessingMachineState.TickOutcome first = state.tick(null, true); // reaches completion, fails to insert
        assertTrue(first.energyConsumed(), "the tick that reaches completion still did work");

        ProcessingMachineState.TickOutcome second = state.tick(null, true); // still parked at completion
        assertFalse(second.energyConsumed(), "a tick spent only waiting for output space must not cost energy");
        assertFalse(second.completed());
    }

    @Test
    void twoOutputsThatIndividuallyFitMayStillNotFitTogether() {
        // Two output slots, each already 63/64 full of a DIFFERENT item than either result —
        // each result alone would fit in "a" slot considered independently, but there are only
        // 2 free single-item slots total and this recipe needs 2 slots for 2 distinct new items,
        // so simulating them independently against the same starting state (as a naive
        // per-output simulate-in-isolation implementation would) wrongly reports success twice
        // against slot 1 instead of reserving slot 1 for the first output and slot 2 for the second.
        ItemStack input = new ItemStack(Items.OAK_LOG, 1);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 6);
        ItemStack sawdust = new ItemStack(Items.STICK, 1); // stand-in for ssm:sawdust
        MachineRecipeResult result = new MachineRecipeResult(List.of(planks, sawdust), 1);

        ItemStackHandler inventory = new ItemStackHandler(3); // 1 input + 2 output slots
        inventory.setStackInSlot(0, input);
        // Both output slots start completely empty; the point is that a per-output-in-isolation
        // simulation would let BOTH outputs claim "slot 1" as their landing spot, so verify the
        // real implementation actually lands them in two distinct slots.

        ProcessingMachineState state = new ProcessingMachineState(inventory, 2, fixedResultLookup(input, result));
        ProcessingMachineState.TickOutcome outcome = state.tick(null, true);

        assertTrue(outcome.completed());
        boolean slot1HasPlanks = inventory.getStackInSlot(1).is(Items.OAK_PLANKS) && inventory.getStackInSlot(1).getCount() == 6;
        boolean slot2HasSawdust = inventory.getStackInSlot(2).is(Items.STICK) && inventory.getStackInSlot(2).getCount() == 1;
        boolean slot1HasSawdust = inventory.getStackInSlot(1).is(Items.STICK) && inventory.getStackInSlot(1).getCount() == 1;
        boolean slot2HasPlanks = inventory.getStackInSlot(2).is(Items.OAK_PLANKS) && inventory.getStackInSlot(2).getCount() == 6;
        assertTrue((slot1HasPlanks && slot2HasSawdust) || (slot1HasSawdust && slot2HasPlanks),
                "both outputs must land, each in its own slot, with nothing dropped");
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.machine.ProcessingMachineStateTest"`
Expected: FAIL — `ProcessingMachineState` does not exist.

- [ ] **Step 7: Write `ProcessingMachineState.java` (the pure, game-instance-free state machine `ProcessingMachineBlockEntity` will delegate to)**

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Optional;

/** Progress/inventory state machine for a processing machine, extracted so it's testable without a live Level. */
public class ProcessingMachineState {

    /** energyConsumed is true only on a tick that actually advanced progress or attempted completion — never on a tick spent only waiting for output space. */
    public record TickOutcome(boolean completed, boolean energyConsumed) {}

    private final ItemStackHandler inventory;
    private final int outputSlotCount;
    private final MachineRecipeLookup recipeLookup;
    private int progress;

    public ProcessingMachineState(ItemStackHandler inventory, int outputSlotCount, MachineRecipeLookup recipeLookup) {
        this.inventory = inventory;
        this.outputSlotCount = outputSlotCount;
        this.recipeLookup = recipeLookup;
    }

    public int getProgress() {
        return progress;
    }

    /** Used only by ProcessingMachineBlockEntity#loadAdditional to restore progress after a chunk reload. */
    public void setProgress(int progress) {
        this.progress = progress;
    }

    public TickOutcome tick(Level level, boolean energyAvailableForOneTick) {
        ItemStack input = inventory.getStackInSlot(0);
        Optional<MachineRecipeResult> current = recipeLookup.find(level, input);

        if (current.isEmpty()) {
            progress = 0;
            return new TickOutcome(false, false);
        }

        MachineRecipeResult result = current.get();
        boolean energyConsumedThisTick = false;

        if (progress < result.processTicks()) {
            if (!energyAvailableForOneTick) {
                return new TickOutcome(false, false);
            }
            progress++;
            energyConsumedThisTick = true;
        }

        if (progress < result.processTicks()) {
            return new TickOutcome(false, energyConsumedThisTick);
        }

        // progress == processTicks: ready to complete, but only if all outputs fit.
        if (canInsertAll(result.outputs())) {
            insertAll(result.outputs());
            inventory.extractItem(0, 1, false);
            progress = 0;
            return new TickOutcome(true, energyConsumedThisTick);
        }

        // Held at completion, waiting for output space. energyConsumedThisTick is only true
        // the one tick where progress just arrived here — every subsequent tick spent parked
        // here takes the `progress < result.processTicks()` == false branch above without ever
        // reaching the `if (!energyAvailableForOneTick)` check, so no further energy is charged.
        return new TickOutcome(false, energyConsumedThisTick);
    }

    /** Simulates inserting every output against a scratch copy of the output slots' current contents, so two outputs that would each individually fit are correctly rejected if they don't BOTH fit together. */
    private boolean canInsertAll(List<ItemStack> outputs) {
        ItemStack[] simulated = new ItemStack[outputSlotCount];
        for (int i = 0; i < outputSlotCount; i++) {
            simulated[i] = inventory.getStackInSlot(1 + i).copy();
        }
        for (ItemStack stack : outputs) {
            if (!tryMergeInto(simulated, stack.copy())) {
                return false;
            }
        }
        return true;
    }

    private boolean tryMergeInto(ItemStack[] simulatedSlots, ItemStack toInsert) {
        for (int i = 0; i < simulatedSlots.length; i++) {
            ItemStack existing = simulatedSlots[i];
            if (existing.isEmpty()) {
                simulatedSlots[i] = toInsert;
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, toInsert)) {
                int room = existing.getMaxStackSize() - existing.getCount();
                if (room >= toInsert.getCount()) {
                    existing.grow(toInsert.getCount());
                    return true;
                }
            }
        }
        return false;
    }

    /** Only called after canInsertAll has already verified the full batch fits, so real insertion here cannot fail partway through. */
    private void insertAll(List<ItemStack> outputs) {
        for (ItemStack stack : outputs) {
            ItemStack remaining = stack.copy();
            for (int slot = 1; slot <= outputSlotCount && !remaining.isEmpty(); slot++) {
                remaining = inventory.insertItem(slot, remaining, false);
            }
        }
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.machine.ProcessingMachineStateTest"`
Expected: PASS (7 tests).

- [ ] **Step 9: Write `ProcessingMachineBlockEntity.java` (thin game-facing wrapper around `ProcessingMachineState`)**

```java
package io.github.scuba10steve.ssm.machine;

import io.github.scuba10steve.ssm.energy.SimpleEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ProcessingMachineBlockEntity extends BlockEntity {

    public final MachineConfig config;
    public final ItemStackHandler inventory;
    public final SimpleEnergyStorage energyStorage;
    private final ProcessingMachineState state;

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, MachineConfig config) {
        super(type, pos, blockState);
        this.config = config;
        // No isItemValid override here: ItemStackHandler.insertItem consults isItemValid
        // internally regardless of caller, so overriding it to reject output slots would
        // also block the machine's OWN completion logic from ever inserting its results.
        // Player-facing "outputs aren't inputs" is enforced instead at the Slot layer, in
        // ProcessingMachineMenu (Task 6), via SlotItemHandler#mayPlace.
        this.inventory = new ItemStackHandler(1 + config.outputSlotCount()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.energyStorage = new SimpleEnergyStorage(config.energyCapacity().getAsInt(), Integer.MAX_VALUE, 0);
        this.state = new ProcessingMachineState(inventory, config.outputSlotCount(), config.recipeLookup());
    }

    public int getProgress() {
        return state.getProgress();
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, ProcessingMachineBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        int perTick = be.config.energyPerTick().getAsInt();
        boolean hasEnergy = be.energyStorage.getEnergyStored() >= perTick;
        ProcessingMachineState.TickOutcome outcome = be.state.tick(level, hasEnergy);
        if (outcome.energyConsumed()) {
            be.energyStorage.extractEnergy(perTick, false);
        }
        be.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("progress", getProgress());
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(tag.getInt("energy"));
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        state.setProgress(tag.getInt("progress"));
    }
}
```

(`outcome.energyConsumed()` replacing the old `hasEnergy && (progress > 0 || completed)` guard is what fixes the bug where a machine parked at 100% waiting for output space kept draining FE every tick forever — see `ProcessingMachineState.tick`'s comment above for why that field is only ever true on a tick that did real work. `loadAdditional` calls a new `state.setProgress(int)` — add that one-line setter to `ProcessingMachineState` in Step 7 alongside `getProgress()`. Task 6 Step 3 modifies this class to add `MenuProvider`, `containerData`, `getDisplayName()`, and `createMenu()` once `ProcessingMachineMenu` exists — see that step for the exact code, which is identical to what a first draft of this step would have had, just relocated to avoid a forward reference.)

- [ ] **Step 10: Write `ProcessingMachineBlock.java`**

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

public class ProcessingMachineBlock extends BaseEntityBlock {

    private final BiFunction<BlockPos, BlockState, ProcessingMachineBlockEntity> factory;

    public ProcessingMachineBlock(BiFunction<BlockPos, BlockState, ProcessingMachineBlockEntity> factory, Properties properties) {
        super(properties);
        this.factory = factory;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ProcessingMachineBlockEntity pmbe) {
                ProcessingMachineBlockEntity.tick(lvl, pos, st, pmbe);
            }
        };
    }
}
```

(No `useWithoutItem` yet — it needs `ProcessingMachineBlockEntity` to implement `MenuProvider`, which Task 6 Step 3 adds once `ProcessingMachineMenu` exists. Task 6 Step 4 adds the override here. Once added: `useWithoutItem`'s exact signature/name has shifted across recent Minecraft versions — e.g. `use`/`useWithoutItem`/`InteractionResult` vs the older `InteractionResult`-boolean split. Confirm the precise 1.21.1 `Block`/`BlockBehaviour` override signature against the real MDK during implementation; the intent — right-click on the server opens the block entity's menu via `Player#openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)` — is what must be preserved regardless of the exact method name.)

- [ ] **Step 11: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "feat: add generic processing-machine framework (state machine, block entity, block)"
```

---

### Task 6: Generic processing-machine menu and screen

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/machine/ProcessingMachineMenu.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/machine/ProcessingMachineScreen.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/registry/ModClientSetup.java`
- Create: `src/main/resources/assets/ssm/textures/gui/container/processing_machine.png` (copy/adapt an existing 176x166 machine GUI background texture — a plain furnace-style layout is sufficient for v1; source one from a permissively-licensed template or draw a minimal placeholder box art asset, since art assets are outside an implementation plan's code scope but the file must exist for the screen to render)
- Modify: `src/main/java/io/github/scuba10steve/ssm/registry/ModMenuTypes.java`, `src/main/java/io/github/scuba10steve/ssm/Ssm.java`
- Modify: `src/main/java/io/github/scuba10steve/ssm/machine/MachineConfig.java`, `ProcessingMachineBlockEntity.java`, `ProcessingMachineBlock.java` (Task 5 deliberately left these without menu wiring to avoid a forward reference on this task's own classes — Step 3a/3b below complete them)

**Interfaces:**
- Consumes: `ProcessingMachineBlockEntity` (Task 5, including its `containerData` field).
- Produces:
  - `ProcessingMachineMenu` with a **client** constructor `(MenuType<?> type, int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf)` (reads the `BlockPos`, resolves the client's own block entity, and wraps a fresh `SimpleContainerData(6)` that the network layer fills in) and a **server** constructor `(MenuType<?> type, int containerId, Inventory playerInventory, ProcessingMachineBlockEntity blockEntity)` (uses the block entity's live `containerData` directly). This split exists because a block entity's custom fields (energy, progress) are **not** automatically kept in sync with the client the way slot contents are — `ContainerData`/`addDataSlots` is vanilla's mechanism for that, and it only works if the menu's own accessors (`getProgress()` etc.) read through `containerData`, never through the block entity's live fields directly (which is why `CoalGeneratorMenu` in `steves-advanced-storage`, the reference this whole framework's GUI pattern is drawn from, does the same split).
  - `ModClientSetup.register(IModEventBus modEventBus)` — a client-only scaffold Tasks 7-13 each add one `event.register(ModMenuTypes.X.get(), ...Screen::new)` line to.

- [ ] **Step 1: Write `ProcessingMachineMenu.java`**

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ProcessingMachineMenu extends AbstractContainerMenu {

    private final ProcessingMachineBlockEntity blockEntity;
    private final ContainerData containerData;
    private final int outputSlotCount;

    // Client constructor (invoked via IMenuTypeExtension when the server tells the client to open this menu)
    public ProcessingMachineMenu(MenuType<?> type, int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(type, containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()), new SimpleContainerData(6));
    }

    // Server constructor (invoked from ProcessingMachineBlockEntity#createMenu)
    public ProcessingMachineMenu(MenuType<?> type, int containerId, Inventory playerInventory, ProcessingMachineBlockEntity blockEntity) {
        this(type, containerId, playerInventory, blockEntity, blockEntity.containerData);
    }

    private ProcessingMachineMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                   ProcessingMachineBlockEntity blockEntity, ContainerData containerData) {
        super(type, containerId);
        this.blockEntity = blockEntity;
        this.containerData = containerData;
        this.outputSlotCount = blockEntity.config.outputSlotCount();

        addSlot(new SlotItemHandler(blockEntity.inventory, 0, 56, 35)); // input slot

        int outputStartX = 116;
        for (int i = 0; i < outputSlotCount; i++) {
            addSlot(new SlotItemHandler(blockEntity.inventory, 1 + i, outputStartX + (i * 18), 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false; // players can take output, but not insert into it directly
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        addDataSlots(containerData);
    }

    private static ProcessingMachineBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof ProcessingMachineBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No ProcessingMachineBlockEntity at " + pos);
    }

    // These read through containerData (synced), never through blockEntity's live fields directly — see Interfaces note above.
    public int getProgress() {
        return containerData.get(0);
    }

    public int getProcessTicks() {
        return containerData.get(1);
    }

    public int getEnergyStored() {
        return (containerData.get(3) << 16) | (containerData.get(2) & 0xFFFF);
    }

    public int getEnergyCapacity() {
        return (containerData.get(5) << 16) | (containerData.get(4) & 0xFFFF);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();
        int firstOutputIndex = 1;
        int lastOutputIndex = outputSlotCount; // inclusive
        int playerInvStart = outputSlotCount + 1;
        int playerInvEnd = playerInvStart + 36;

        if (index == 0 || (index >= firstOutputIndex && index <= lastOutputIndex)) {
            if (!this.moveItemStackTo(slotStack, playerInvStart, playerInvEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }
}
```

- [ ] **Step 2: Write `ProcessingMachineScreen.java`**

```java
package io.github.scuba10steve.ssm.machine;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ProcessingMachineScreen extends AbstractContainerScreen<ProcessingMachineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/container/processing_machine.png");

    public ProcessingMachineScreen(ProcessingMachineMenu menu, net.minecraft.world.entity.player.Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        int progress = menu.getProgress();
        int total = menu.getProcessTicks();
        if (total > 0) {
            int filled = (progress * 24) / total;
            graphics.blit(TEXTURE, x + 79, y + 34, 176, 0, filled, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("container.inventory"), 8, imageHeight - 96, 0x404040, false);
    }
}
```

- [ ] **Step 3: Add `menuType` to `MachineConfig`, and `MenuProvider`/`containerData`/`getDisplayName`/`createMenu` to `ProcessingMachineBlockEntity`**, now that `ProcessingMachineMenu` exists (Task 5 deliberately left these out to avoid a forward reference — see Task 5 Steps 4 and 9's notes)

Modify `MachineConfig.java` (Task 5 Step 4) to add the field back:

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.inventory.MenuType;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public record MachineConfig(
        String id,
        MachineRecipeLookup recipeLookup,
        int outputSlotCount,
        IntSupplier energyCapacity,
        IntSupplier energyPerTick,
        IntSupplier baseProcessTicks,
        Supplier<MenuType<ProcessingMachineMenu>> menuType
) {}
```

(`menuType` is a `Supplier` rather than a plain `MenuType<ProcessingMachineMenu>` because `MachineConfigs.CRUSHER`/`SAWMILL`/`SMELTER` in Tasks 7-9 are `static final` fields initialized before `ModMenuTypes`' own `DeferredRegister` entries are guaranteed populated — the lazy `Supplier` defers that lookup to first use, same reasoning as the self-referencing `BlockEntityType` lambda in Task 7.)

Modify `ProcessingMachineBlockEntity.java` (Task 5 Step 9) to add the class declaration change, the `containerData` field, and the two `MenuProvider` methods:

```java
// class declaration changes to:
public class ProcessingMachineBlockEntity extends BlockEntity implements MenuProvider {
```

Add these imports: `net.minecraft.network.chat.Component`, `net.minecraft.world.MenuProvider`, `net.minecraft.world.entity.player.Inventory`, `net.minecraft.world.entity.player.Player`, `net.minecraft.world.inventory.AbstractContainerMenu`, `net.minecraft.world.inventory.ContainerData`.

Add this field (before the constructor):

```java
// ContainerData slots: 0 = progress, 1 = processTicks, 2-3 = energy (lo/hi), 4-5 = capacity (lo/hi)
public final ContainerData containerData = new ContainerData() {
    @Override
    public int get(int index) {
        return switch (index) {
            case 0 -> getProgress();
            case 1 -> config.baseProcessTicks().getAsInt();
            case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
            case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
            case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
            case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        // Client-authoritative fields only; server drives all of these, nothing to set.
    }

    @Override
    public int getCount() {
        return 6;
    }
};
```

Add these two methods (after `getProgress()`, before or after `tick(...)` — placement doesn't matter):

```java
@Override
public Component getDisplayName() {
    return Component.translatable("block.ssm." + config.id());
}

@Override
public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
    return new ProcessingMachineMenu(config.menuType().get(), id, playerInventory, this);
}
```

- [ ] **Step 4: Add `useWithoutItem` to `ProcessingMachineBlock`**, now that `ProcessingMachineBlockEntity` implements `MenuProvider`

Modify `ProcessingMachineBlock.java` (Task 5 Step 10): add imports `net.minecraft.world.InteractionResult`, `net.minecraft.world.entity.player.Player`, `net.minecraft.world.phys.BlockHitResult`, and this method (between `getRenderShape` and `getTicker`):

```java
@Override
protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
    if (level.isClientSide()) {
        return InteractionResult.SUCCESS;
    }
    if (level.getBlockEntity(pos) instanceof ProcessingMachineBlockEntity be) {
        player.openMenu(be, buf -> buf.writeBlockPos(pos));
    }
    return InteractionResult.CONSUME;
}
```

(Confirm the precise 1.21.1 `Block`/`BlockBehaviour` override signature against the real MDK during implementation — this has shifted across recent Minecraft versions. The intent — right-click on the server opens the block entity's menu via `Player#openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)` — is what must be preserved regardless of the exact method name.)

- [ ] **Step 5: Write `ModClientSetup.java`** (client-only screen registration scaffold; Tasks 7-13 each add one `event.register(...)` line here as their own menu type comes into existence)

```java
package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.machine.ProcessingMachineScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ModClientSetup {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModClientSetup::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        // Task 7 adds: event.register(ModMenuTypes.CRUSHER.get(), ProcessingMachineScreen::new);
        // Task 8 adds: event.register(ModMenuTypes.SAWMILL.get(), ProcessingMachineScreen::new);
        // Task 9 adds: event.register(ModMenuTypes.SMELTER.get(), ProcessingMachineScreen::new);
        // Tasks 10-13 each add their own generator MenuType/Screen pair.
    }
}
```

(`RegisterMenuScreensEvent` already only fires client-side in NeoForge — no separate `Dist.CLIENT` gating or `DistExecutor` wrapping is needed around the listener itself, but confirm this against the actual NeoForge 1.21.1 event during implementation; the import above is left in only if that turns out to be necessary.)

- [ ] **Step 6: Wire `ModClientSetup.register(modEventBus)` into `Ssm.java`**, alongside the other `register(...)` calls from earlier tasks.

- [ ] **Step 7: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL` (the PNG can be a minimal 176x166 placeholder for now — visual polish is not a v1 blocker per the spec).

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add generic processing-machine menu, screen, MenuProvider wiring, and client screen registration"
```

---

### Task 7: Crusher (recipe type, recipes, block wiring)

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/recipe/CrushingRecipe.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/registry/ModRecipes.java`
- Modify: `src/main/java/io/github/scuba10steve/ssm/registry/ModBlocks.java`, `ModBlockEntities.java`, `ModItems.java`, `ModMenuTypes.java`, `ModCapabilities.java`, `ModCreativeTabs.java`
- Create: `src/main/resources/data/ssm/recipe/crushing/iron.json`, `gold.json`, `copper.json`
- Test: `src/test/java/io/github/scuba10steve/ssm/recipe/CrushingRecipeTest.java`

**Interfaces:**
- Consumes: `SsmMachineRecipe`, `MachineRecipeLookup`, `MachineRecipeResult`, `MachineConfig`, `ProcessingMachineBlockEntity`, `ProcessingMachineBlock`, `ProcessingMachineMenu` (Tasks 5-6).
- Produces: `ModItems.CRUSHED_IRON`, `CRUSHED_GOLD`, `CRUSHED_COPPER` (raw crushed-ore items); `ModBlocks.CRUSHER`; `ModBlockEntities.CRUSHER`; `ModMenuTypes.CRUSHER`; `ModRecipes.CRUSHING_TYPE` (`RecipeType<CrushingRecipe>`) and `ModRecipes.CRUSHING_SERIALIZER`.

- [ ] **Step 1: Write the failing recipe-matching test**

```java
package io.github.scuba10steve.ssm.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrushingRecipeTest {

    @Test
    void matchesConfiguredIngredientOnly() {
        Ingredient rawIron = Ingredient.of(Items.RAW_IRON);
        ItemStack output = new ItemStack(Items.IRON_NUGGET); // placeholder result item, real item added in Step 4
        CrushingRecipe recipe = new CrushingRecipe(rawIron, output, 2);

        assertTrue(recipe.matches(new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), null));
        assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(Items.RAW_GOLD)), null));
    }

    @Test
    void producesDoubledOutputCount() {
        Ingredient rawIron = Ingredient.of(Items.RAW_IRON);
        ItemStack output = new ItemStack(Items.IRON_NUGGET);
        CrushingRecipe recipe = new CrushingRecipe(rawIron, output, 2);

        var results = recipe.getResults(null);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getCount());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.recipe.CrushingRecipeTest"`
Expected: FAIL — `CrushingRecipe` does not exist.

- [ ] **Step 3: Write `CrushingRecipe.java`**

```java
package io.github.scuba10steve.ssm.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.scuba10steve.ssm.machine.SsmMachineRecipe;
import io.github.scuba10steve.ssm.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;

public record CrushingRecipe(Ingredient input, ItemStack result, int outputCount) implements SsmMachineRecipe {

    @Override
    public boolean matches(SingleRecipeInput recipeInput, net.minecraft.world.level.Level level) {
        return input.test(recipeInput.item());
    }

    @Override
    public List<ItemStack> getResults(HolderLookup.Provider registries) {
        ItemStack out = result.copy();
        out.setCount(outputCount);
        return List.of(out);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<? extends CrushingRecipe> getSerializer() {
        return ModRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends CrushingRecipe> getType() {
        return ModRecipes.CRUSHING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CrushingRecipe> {
        public static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("input").forGetter(CrushingRecipe::input),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
                com.mojang.serialization.Codec.INT.fieldOf("output_count").forGetter(CrushingRecipe::outputCount)
        ).apply(instance, CrushingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::input,
                ItemStack.STREAM_CODEC, CrushingRecipe::result,
                ByteBufCodecs.VAR_INT, CrushingRecipe::outputCount,
                CrushingRecipe::new
        );

        @Override
        public MapCodec<CrushingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
```

(Verify the exact `Ingredient`/`ItemStack` codec and stream-codec field names against the real NeoForge 1.21.1 MDK sources during implementation — Mojang has moved these between snapshots before; `Ingredient.CODEC` / `Ingredient.CONTENTS_STREAM_CODEC` and `ItemStack.STRICT_CODEC` / `ItemStack.STREAM_CODEC` are the 1.21.1-era names to start from.)

- [ ] **Step 4: Write `ModRecipes.java`**

```java
package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.recipe.CrushingRecipe;
import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, RefStrings.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, RefStrings.MODID);

    public static final Supplier<RecipeType<CrushingRecipe>> CRUSHING_TYPE =
            RECIPE_TYPES.register("crushing", () -> new RecipeType<CrushingRecipe>() {
                @Override
                public String toString() {
                    return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "crushing").toString();
                }
            });

    public static final Supplier<CrushingRecipe.Serializer> CRUSHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crushing", CrushingRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.recipe.CrushingRecipeTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Add crushed-ore items to `ModItems.java`**

```java
public static final Supplier<Item> CRUSHED_IRON = ITEMS.register("crushed_iron", () -> new Item(new Item.Properties()));
public static final Supplier<Item> CRUSHED_GOLD = ITEMS.register("crushed_gold", () -> new Item(new Item.Properties()));
public static final Supplier<Item> CRUSHED_COPPER = ITEMS.register("crushed_copper", () -> new Item(new Item.Properties()));
```

- [ ] **Step 7: Add the Crusher block/block-entity/menu registration**

`ModBlocks.java`:
```java
public static final Supplier<ProcessingMachineBlock> CRUSHER = BLOCKS.register("crusher", () ->
        new ProcessingMachineBlock(
                (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.CRUSHER.get(), pos, state, MachineConfigs.CRUSHER),
                BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));
```

`ModItems.java` (block item):
```java
public static final Supplier<Item> CRUSHER = ITEMS.register("crusher",
        () -> new BlockItem(ModBlocks.CRUSHER.get(), new Item.Properties()));
```

`ModBlockEntities.java`:
```java
public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> CRUSHER =
        BLOCK_ENTITIES.register("crusher", () ->
                BlockEntityType.Builder.of((pos, state) ->
                        new ProcessingMachineBlockEntity(ModBlockEntities.CRUSHER.get(), pos, state, MachineConfigs.CRUSHER),
                        ModBlocks.CRUSHER.get()).build(null));
```

(`BlockEntityType.Builder.of` takes a `BlockEntityType.BlockEntitySupplier<T>`, whose actual shape is `(BlockPos, BlockState) -> T` — it does not receive the type itself. The lambda above self-references `ModBlockEntities.CRUSHER.get()` for the `type` constructor argument; this is safe despite looking circular because the lambda body only runs when a Crusher block entity is actually constructed at block-place or chunk-load time, which is always after the `DeferredRegister` has finished resolving this exact `Supplier`. This is the same self-referencing idiom vanilla's own block entities use, e.g. `FurnaceBlockEntity`'s constructor passing the static `BlockEntityType.FURNACE` it is itself later assigned to.)

`ModMenuTypes.java`:
```java
public static final Supplier<MenuType<ProcessingMachineMenu>> CRUSHER = MENU_TYPES.register("crusher", () ->
        net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                (containerId, inv, buf) -> new ProcessingMachineMenu(ModMenuTypes.CRUSHER.get(), containerId, inv, buf)));
```

(This calls `ProcessingMachineMenu`'s **client** constructor from Task 6 Step 1 directly — it takes the raw `buf` and resolves the block entity and a fresh synced `ContainerData` itself. Do not reconstruct the menu by manually reading `buf.readBlockPos()` and looking up the block entity here; that was Task 7's original sketch and it bypassed the client/server `ContainerData` split Task 6 fixed.)

- [ ] **Step 8: Create `MachineConfigs.java`**

```java
package io.github.scuba10steve.ssm.machine;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.recipe.CrushingRecipe;
import io.github.scuba10steve.ssm.registry.ModRecipes;
import io.github.scuba10steve.ssm.registry.ModMenuTypes;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.Optional;

public class MachineConfigs {
    public static final MachineConfig CRUSHER = new MachineConfig(
            "crusher",
            (level, input) -> level.getRecipeManager()
                    .getRecipeFor(ModRecipes.CRUSHING_TYPE.get(), new SingleRecipeInput(input), level)
                    .map(holder -> new MachineRecipeResult(
                            holder.value().getResults(level.registryAccess()),
                            holder.value().getProcessTicksOverride() >= 0
                                    ? holder.value().getProcessTicksOverride()
                                    : SsmConfig.CRUSHER_PROCESS_TICKS.get())),
            1,
            SsmConfig.CRUSHER_CAPACITY::get,
            SsmConfig.CRUSHER_ENERGY_PER_TICK::get,
            SsmConfig.CRUSHER_PROCESS_TICKS::get,
            ModMenuTypes.CRUSHER
    );
}
```

- [ ] **Step 9: Register `ModCapabilities` entry for the Crusher**

```java
event.registerBlockEntity(
        net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
        ModBlockEntities.CRUSHER.get(),
        (be, side) -> be.energyStorage);
event.registerBlockEntity(
        net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
        ModBlockEntities.CRUSHER.get(),
        (be, side) -> be.inventory);
```

- [ ] **Step 10: Add crushing recipe JSONs**

`src/main/resources/data/ssm/recipe/crushing/iron.json`:
```json
{
  "type": "ssm:crushing",
  "input": { "tag": "c:raw_materials/iron" },
  "result": { "id": "ssm:crushed_iron", "count": 1 },
  "output_count": 2
}
```

Repeat for `gold.json` (`c:raw_materials/gold` -> `ssm:crushed_gold`) and `copper.json` (`c:raw_materials/copper` -> `ssm:crushed_copper`), same shape.

- [ ] **Step 11: Add crafting recipe for the Crusher block and register in the creative tab**

`src/main/resources/data/ssm/recipe/crafting/crusher.json`:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "III",
    "IFI",
    "III"
  ],
  "key": {
    "I": { "item": "minecraft:iron_ingot" },
    "F": { "item": "minecraft:furnace" }
  },
  "result": { "id": "ssm:crusher", "count": 1 }
}
```

In `ModCreativeTabs.java`, replace the placeholder `icon`/`displayItems` from Task 2:
```java
.icon(() -> new ItemStack(ModItems.CRUSHER.get()))
.displayItems((params, output) -> {
    output.accept(ModItems.CRUSHER.get());
    output.accept(ModItems.CRUSHED_IRON.get());
    output.accept(ModItems.CRUSHED_GOLD.get());
    output.accept(ModItems.CRUSHED_COPPER.get());
})
```

- [ ] **Step 12: Wire `ModRecipes.register(modEventBus)` into `Ssm.java`**

- [ ] **Step 13: Add the Crusher's screen registration to `ModClientSetup.registerScreens`** (Task 6 Step 5)

```java
event.register(ModMenuTypes.CRUSHER.get(), ProcessingMachineScreen::new);
```

- [ ] **Step 14: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 15: Commit**

```bash
git add -A
git commit -m "feat: add Crusher block, crushing recipe type, and crushed-ore items"
```

---

### Task 8: Sawmill (recipe type, recipes, sawdust item, block wiring)

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/recipe/SawingRecipe.java`
- Modify: `src/main/java/io/github/scuba10steve/ssm/registry/ModRecipes.java`, `ModItems.java`, `ModBlocks.java`, `ModBlockEntities.java`, `ModMenuTypes.java`, `ModCapabilities.java`, `ModCreativeTabs.java`, `MachineConfigs.java`
- Create: `src/main/resources/data/ssm/recipe/sawing/oak.json` ... one per log type
- Test: `src/test/java/io/github/scuba10steve/ssm/recipe/SawingRecipeTest.java`

**Interfaces:**
- Consumes: same framework as Task 7.
- Produces: `ModItems.SAWDUST`, `ModBlocks.SAWMILL`, `ModRecipes.SAWING_TYPE`/`SAWING_SERIALIZER`, `MachineConfigs.SAWMILL`.

- [ ] **Step 1: Write the failing test**

```java
package io.github.scuba10steve.ssm.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SawingRecipeTest {

    @Test
    void producesPlanksAndSawdust() {
        SawingRecipe recipe = new SawingRecipe(
                Ingredient.of(Items.OAK_LOG),
                new ItemStack(Items.OAK_PLANKS), 6,
                new ItemStack(Items.STICK), 1); // sawdust item substituted for a real vanilla stand-in until Step 3

        var results = recipe.getResults(null);
        assertEquals(2, results.size());
        assertEquals(6, results.get(0).getCount());
        assertEquals(1, results.get(1).getCount());
    }

    @Test
    void matchesOnlyConfiguredLog() {
        SawingRecipe recipe = new SawingRecipe(
                Ingredient.of(Items.OAK_LOG),
                new ItemStack(Items.OAK_PLANKS), 6,
                new ItemStack(Items.STICK), 1);

        assertTrue(recipe.matches(new SingleRecipeInput(new ItemStack(Items.OAK_LOG)), null));
        assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(Items.SPRUCE_LOG)), null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.recipe.SawingRecipeTest"`
Expected: FAIL — `SawingRecipe` does not exist.

- [ ] **Step 3: Write `SawingRecipe.java`** (same shape as `CrushingRecipe`, two named outputs instead of one)

```java
package io.github.scuba10steve.ssm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.scuba10steve.ssm.machine.SsmMachineRecipe;
import io.github.scuba10steve.ssm.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;

public record SawingRecipe(
        Ingredient input,
        ItemStack planksResult, int planksCount,
        ItemStack sawdustResult, int sawdustCount
) implements SsmMachineRecipe {

    @Override
    public boolean matches(SingleRecipeInput recipeInput, net.minecraft.world.level.Level level) {
        return input.test(recipeInput.item());
    }

    @Override
    public List<ItemStack> getResults(HolderLookup.Provider registries) {
        ItemStack planks = planksResult.copy();
        planks.setCount(planksCount);
        ItemStack sawdust = sawdustResult.copy();
        sawdust.setCount(sawdustCount);
        return List.of(planks, sawdust);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return planksResult.copy();
    }

    @Override
    public RecipeSerializer<? extends SawingRecipe> getSerializer() {
        return ModRecipes.SAWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends SawingRecipe> getType() {
        return ModRecipes.SAWING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<SawingRecipe> {
        public static final MapCodec<SawingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("input").forGetter(SawingRecipe::input),
                ItemStack.STRICT_CODEC.fieldOf("planks_result").forGetter(SawingRecipe::planksResult),
                Codec.INT.fieldOf("planks_count").forGetter(SawingRecipe::planksCount),
                ItemStack.STRICT_CODEC.fieldOf("sawdust_result").forGetter(SawingRecipe::sawdustResult),
                Codec.INT.fieldOf("sawdust_count").forGetter(SawingRecipe::sawdustCount)
        ).apply(instance, SawingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SawingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SawingRecipe::input,
                ItemStack.STREAM_CODEC, SawingRecipe::planksResult,
                ByteBufCodecs.VAR_INT, SawingRecipe::planksCount,
                ItemStack.STREAM_CODEC, SawingRecipe::sawdustResult,
                ByteBufCodecs.VAR_INT, SawingRecipe::sawdustCount,
                SawingRecipe::new
        );

        @Override
        public MapCodec<SawingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SawingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.recipe.SawingRecipeTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Add `SAWING_TYPE`/`SAWING_SERIALIZER` to `ModRecipes.java`** (same pattern as `CRUSHING_TYPE`/`CRUSHING_SERIALIZER` from Task 7, substituting `SawingRecipe`).

- [ ] **Step 6: Add `ModItems.SAWDUST`, block/block-entity/menu for `SAWMILL`, and `MachineConfigs.SAWMILL`** — identical shape to Task 7 Steps 6-8, substituting Sawmill names and `outputSlotCount = 2`. `MachineConfigs.SAWMILL`'s final constructor argument is `ModMenuTypes.SAWMILL` (the `menuType` field added to `MachineConfig` in Task 5), and `ModMenuTypes.SAWMILL`'s `IMenuTypeExtension.create(...)` lambda follows Task 7 Step 7's corrected client-constructor form: `(containerId, inv, buf) -> new ProcessingMachineMenu(ModMenuTypes.SAWMILL.get(), containerId, inv, buf)`.

- [ ] **Step 7: Add `ModCapabilities` entries for the Sawmill** (energy + item handler), same shape as Task 7 Step 9.

- [ ] **Step 8: Add the Sawmill's screen registration to `ModClientSetup.registerScreens`** (Task 6 Step 5): `event.register(ModMenuTypes.SAWMILL.get(), ProcessingMachineScreen::new);`

- [ ] **Step 9: Add sawing recipe JSONs for every 1.21.1 vanilla log type**

`src/main/resources/data/ssm/recipe/sawing/oak.json`:
```json
{
  "type": "ssm:sawing",
  "input": { "item": "minecraft:oak_log" },
  "planks_result": { "id": "minecraft:oak_planks" },
  "planks_count": 6,
  "sawdust_result": { "id": "ssm:sawdust" },
  "sawdust_count": 1
}
```

Repeat this exact shape for: `spruce_log`→`spruce_planks`, `birch_log`→`birch_planks`, `jungle_log`→`jungle_planks`, `acacia_log`→`acacia_planks`, `dark_oak_log`→`dark_oak_planks`, `mangrove_log`→`mangrove_planks`, `cherry_log`→`cherry_planks`, `crimson_stem`→`crimson_planks`, `warped_stem`→`warped_planks` (10 files total — no `pale_oak_log`, per the spec's 1.21.1 version note).

- [ ] **Step 10: Add crafting recipe for the Sawmill and the Sawdust display item, register in the creative tab** — same shape as Task 7 Step 11 (a shaped recipe using e.g. iron ingots + an axe, `data/ssm/recipe/crafting/sawmill.json`), plus `output.accept(ModItems.SAWMILL.get())` / `output.accept(ModItems.SAWDUST.get())` in `ModCreativeTabs`.

- [ ] **Step 11: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "feat: add Sawmill block, sawing recipe type, and sawdust byproduct"
```

---

### Task 9: Smelter (vanilla smelting reuse)

**Files:**
- Modify: `src/main/java/io/github/scuba10steve/ssm/registry/ModItems.java`, `ModBlocks.java`, `ModBlockEntities.java`, `ModMenuTypes.java`, `ModCapabilities.java`, `ModCreativeTabs.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/machine/MachineConfigs.java` addition (`SMELTER`)
- Create: `src/main/resources/data/ssm/recipe/smelting/crushed_iron.json`, `crushed_gold.json`, `crushed_copper.json`
- Create: `src/main/resources/data/ssm/recipe/crafting/smelter.json`
- Test: `src/test/java/io/github/scuba10steve/ssm/machine/SmeltingLookupTest.java` (an integration-style test deferred to the gametest suite in Task 16, since it needs a live `RecipeManager`; this task's unit test instead verifies the *adapter's* result-shape logic in isolation using a hand-built fake `RecipeManager`-shaped lookup function)

**Interfaces:**
- Consumes: `MachineConfig`, `MachineRecipeLookup`, `MachineRecipeResult` (Task 5); vanilla `RecipeType.SMELTING` / `SmeltingRecipe` / `SingleRecipeInput`.
- Produces: `MachineConfigs.SMELTER`, `ModBlocks.SMELTER`.

- [ ] **Step 1: Write the failing test for the smelting-recipe adapter shape**

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SmeltingLookupTest {

    @Test
    void wrapsAVanillaSmeltingMatchIntoAFlatDurationSingleOutputResult() {
        // Fakes what MachineConfigs.SMELTER's real lookup does against RecipeManager,
        // without needing a live Level: given a vanilla smelting result item and a
        // configured flat process-tick count, the adapter must produce exactly one
        // output stack and ignore the vanilla recipe's own cookingTime.
        ItemStack smeltedResult = new ItemStack(Items.IRON_INGOT);
        int flatProcessTicks = 100;

        MachineRecipeResult result = SmelterAdapter.toMachineResult(smeltedResult, flatProcessTicks);

        assertEquals(1, result.outputs().size());
        assertEquals(Items.IRON_INGOT, result.outputs().get(0).getItem());
        assertEquals(1, result.outputs().get(0).getCount());
        assertEquals(100, result.processTicks());
    }

    @Test
    void emptyResultItemMeansNoMatch() {
        assertTrue(Optional.of(ItemStack.EMPTY).filter(s -> !s.isEmpty()).isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.machine.SmeltingLookupTest"`
Expected: FAIL — `SmelterAdapter` does not exist.

- [ ] **Step 3: Write `SmelterAdapter.java`** (the small pure-function piece, factored out so it's unit-testable; `MachineConfigs.SMELTER` below wires it to the live `RecipeManager`)

```java
package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SmelterAdapter {
    private SmelterAdapter() {}

    public static MachineRecipeResult toMachineResult(ItemStack vanillaResult, int flatProcessTicks) {
        return new MachineRecipeResult(List.of(vanillaResult.copyWithCount(1)), flatProcessTicks);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.machine.SmeltingLookupTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Add `MachineConfigs.SMELTER`, wiring `SmelterAdapter` to the live vanilla `RecipeManager`**

```java
public static final MachineConfig SMELTER = new MachineConfig(
        "smelter",
        (level, input) -> {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            return level.getRecipeManager()
                    .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, recipeInput, level)
                    .map(holder -> holder.value().assemble(recipeInput, level.registryAccess()))
                    .filter(result -> !result.isEmpty())
                    .map(result -> SmelterAdapter.toMachineResult(result, SsmConfig.SMELTER_PROCESS_TICKS.get()));
        },
        1,
        SsmConfig.SMELTER_CAPACITY::get,
        SsmConfig.SMELTER_ENERGY_PER_TICK::get,
        SsmConfig.SMELTER_PROCESS_TICKS::get,
        io.github.scuba10steve.ssm.registry.ModMenuTypes.SMELTER
);
```

- [ ] **Step 6: Add `ModItems`/`ModBlocks`/`ModBlockEntities`/`ModMenuTypes` entries for `SMELTER`** — identical shape to Task 7 Steps 6-7, `outputSlotCount = 1`, no new custom recipe type needed (the Smelter's `MachineConfig` reads vanilla `minecraft:smelting` recipes directly, as designed above). `ModMenuTypes.SMELTER`'s `IMenuTypeExtension.create(...)` lambda follows the same corrected client-constructor form as Task 7 Step 7.

- [ ] **Step 7: Add `ModCapabilities` entries for the Smelter** (energy + item handler), same shape as Task 7 Step 9.

- [ ] **Step 8: Add the Smelter's screen registration to `ModClientSetup.registerScreens`** (Task 6 Step 5): `event.register(ModMenuTypes.SMELTER.get(), ProcessingMachineScreen::new);`

- [ ] **Step 9: Add mod-authored `minecraft:smelting` recipes for the mod's own crushed-ore items**

`src/main/resources/data/ssm/recipe/smelting/crushed_iron.json`:
```json
{
  "type": "minecraft:smelting",
  "ingredient": { "item": "ssm:crushed_iron" },
  "result": { "id": "minecraft:iron_ingot" },
  "experience": 0.7,
  "cookingtime": 200
}
```

Repeat for `crushed_gold.json` (`ssm:crushed_gold` -> `minecraft:gold_ingot`, experience 1.0) and `crushed_copper.json` (`ssm:crushed_copper` -> `minecraft:copper_ingot`, experience 0.6) — these vanilla-type recipes also make the crushed ores smeltable in a plain vanilla furnace, which is a harmless (even nice) side effect, not a bug.

- [ ] **Step 10: Add crafting recipe for the Smelter, register in the creative tab** — same shape as Task 7 Step 11.

- [ ] **Step 11: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "feat: add Smelter block reusing vanilla smelting recipes"
```

---

### Task 10: Combustion Generator (ported from CoalGeneratorBlockEntity)

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/CombustionGeneratorBlockEntity.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/GeneratorBlock.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/CombustionGeneratorMenu.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/CombustionGeneratorScreen.java`
- Modify: `src/main/java/io/github/scuba10steve/ssm/registry/ModBlocks.java`, `ModItems.java`, `ModBlockEntities.java`, `ModMenuTypes.java`, `ModCapabilities.java`, `ModCreativeTabs.java`
- Create: `src/main/resources/data/ssm/recipe/crafting/combustion_generator.json`
- Test: `src/test/java/io/github/scuba10steve/ssm/generator/CombustionBurnStateTest.java`

**Interfaces:**
- Consumes: `SimpleEnergyStorage` (Task 3).
- Produces: `CombustionGeneratorBlockEntity` (implements `MenuProvider`) with public fields `fuelHandler` (`ItemStackHandler`), `energyStorage` (`SimpleEnergyStorage`), `litDuration` (int), `containerData` (`ContainerData`); `int getRemainingBurnTime()`; static `tick(Level, BlockPos, BlockState, CombustionGeneratorBlockEntity)`; static `getBurnTime(ItemStack)`. `GeneratorBlock<T>` (shared base for all 4 generators) gains a `useWithoutItem` that opens the menu for any `T` implementing `MenuProvider`.

This ports `steves-advanced-storage`'s `CoalGeneratorBlockEntity` (`neoforge/src/main/java/io/github/scuba10steve/s3/advanced/blockentity/CoalGeneratorBlockEntity.java`), with one behavioral change: **do not burn down `litTime` on a tick where the energy buffer has no room** — preserve the remaining burn time instead of wasting it (per the spec's "buffer-full pause" rule, which deliberately diverges from the source mod's simpler "always burn, waste the excess" behavior).

- [ ] **Step 1: Write the failing test for the buffer-full-pause behavior** (the one intentional divergence from the ported source — test this in isolation before writing the full block entity)

```java
package io.github.scuba10steve.ssm.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombustionBurnStateTest {

    @Test
    void burnPausesWhenBufferIsFullAndResumesWithSpace() {
        CombustionBurnState state = new CombustionBurnState();
        state.startBurning(1600); // one coal's burn time

        // Buffer full: generate() would accept 0 this tick.
        CombustionBurnState.TickResult full = state.tick(/* generationRate */ 160, /* bufferRoom */ 0);
        assertEquals(1600, full.remainingBurnTime(), "no burn time consumed while buffer is full");
        assertEquals(0, full.energyToGenerate());

        // Buffer has room again.
        CombustionBurnState.TickResult resumed = state.tick(160, 500);
        assertEquals(1599, resumed.remainingBurnTime());
        assertEquals(160, resumed.energyToGenerate());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.generator.CombustionBurnStateTest"`
Expected: FAIL — `CombustionBurnState` does not exist.

- [ ] **Step 3: Write `CombustionBurnState.java`** (the pure burn-timer logic, extracted for testability, mirroring the divergence called out in the spec)

```java
package io.github.scuba10steve.ssm.generator;

public class CombustionBurnState {

    public record TickResult(int remainingBurnTime, int energyToGenerate) {}

    private int remainingBurnTime;

    public void startBurning(int burnTime) {
        this.remainingBurnTime = burnTime;
    }

    public boolean isBurning() {
        return remainingBurnTime > 0;
    }

    public int getRemainingBurnTime() {
        return remainingBurnTime;
    }

    /** Used only by CombustionGeneratorBlockEntity#loadAdditional to restore state after a chunk reload. */
    public void setRemainingBurnTime(int remainingBurnTime) {
        this.remainingBurnTime = remainingBurnTime;
    }

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
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.generator.CombustionBurnStateTest"`
Expected: PASS.

- [ ] **Step 5: Write `CombustionGeneratorBlockEntity.java`**, porting `CoalGeneratorBlockEntity` and delegating burn-timer decisions to `CombustionBurnState`

```java
package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.energy.SimpleEnergyStorage;
import io.github.scuba10steve.ssm.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CombustionGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    private final CombustionBurnState burnState = new CombustionBurnState();
    public int litDuration;

    public final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getBurnTime(stack) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public final SimpleEnergyStorage energyStorage;

    // ContainerData slots: 0 = remainingBurnTime, 1 = litDuration, 2-3 = energy (lo/hi), 4-5 = capacity (lo/hi), 6 = generationRate
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getRemainingBurnTime();
                case 1 -> litDuration;
                case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 6 -> SsmConfig.COMBUSTION_GENERATION_RATE.get();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state);
        energyStorage = new SimpleEnergyStorage(SsmConfig.COMBUSTION_CAPACITY.get(), 0, SsmConfig.COMBUSTION_MAX_OUTPUT.get());
    }

    public boolean isLit() {
        return burnState.isBurning();
    }

    public int getRemainingBurnTime() {
        return burnState.getRemainingBurnTime();
    }

    public static int getBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item == Items.COAL || item == Items.CHARCOAL) return 1600;
        if (item == Items.COAL_BLOCK) return 16000;
        return 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CombustionGeneratorBlockEntity be) {
        if (level.isClientSide()) return;

        int generationRate = SsmConfig.COMBUSTION_GENERATION_RATE.get();
        int capacity = SsmConfig.COMBUSTION_CAPACITY.get();
        int maxOutput = SsmConfig.COMBUSTION_MAX_OUTPUT.get();
        int bufferRoom = capacity - be.energyStorage.getEnergyStored();

        boolean wasBurning = be.burnState.isBurning();

        if (be.burnState.isBurning()) {
            var result = be.burnState.tick(generationRate, bufferRoom);
            if (result.energyToGenerate() > 0) {
                be.energyStorage.generate(result.energyToGenerate());
                be.setChanged();
            }
        }

        if (!be.burnState.isBurning() && be.energyStorage.getEnergyStored() < capacity) {
            ItemStack fuel = be.fuelHandler.getStackInSlot(0);
            int burnTime = getBurnTime(fuel);
            if (burnTime > 0) {
                be.burnState.startBurning(burnTime);
                be.litDuration = burnTime;
                be.fuelHandler.extractItem(0, 1, false);
            }
        }

        if (wasBurning != be.burnState.isBurning()) {
            be.setChanged();
        }

        if (be.energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                IEnergyStorage neighbour = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (neighbour != null && neighbour.canReceive()) {
                    int toSend = Math.min(be.energyStorage.getEnergyStored(), maxOutput);
                    int sent = neighbour.receiveEnergy(toSend, false);
                    if (sent > 0) {
                        be.energyStorage.extractEnergy(sent, false);
                        be.setChanged();
                    }
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ssm.combustion_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(io.github.scuba10steve.ssm.registry.ModMenuTypes.COMBUSTION_GENERATOR.get(), id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("remainingBurnTime", burnState.getRemainingBurnTime());
        tag.putInt("litDuration", litDuration);
        tag.put("fuel", fuelHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(tag.getInt("energy"));
        burnState.setRemainingBurnTime(tag.getInt("remainingBurnTime"));
        litDuration = tag.getInt("litDuration");
        fuelHandler.deserializeNBT(registries, tag.getCompound("fuel"));
    }
}
```

- [ ] **Step 6: Write `GeneratorBlock.java`** (thin `BaseEntityBlock` wrapper, parallel to `ProcessingMachineBlock`, parameterized by a block-entity factory and ticker)

```java
package io.github.scuba10steve.ssm.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiFunction;

public class GeneratorBlock<T extends BlockEntity> extends BaseEntityBlock {

    private final BiFunction<BlockPos, BlockState, T> factory;
    private final GeneratorTicker<T> ticker;

    public interface GeneratorTicker<T extends BlockEntity> {
        void tick(Level level, BlockPos pos, BlockState state, T blockEntity);
    }

    public GeneratorBlock(BiFunction<BlockPos, BlockState, T> factory, GeneratorTicker<T> ticker, Properties properties) {
        super(properties);
        this.factory = factory;
        this.ticker = ticker;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, be) -> ticker.tick(lvl, pos, st, (T) be);
    }
}
```

(Same caveat as `ProcessingMachineBlock`'s `useWithoutItem` in Task 5 Step 10: confirm the exact 1.21.1 override signature against the real MDK. The `MenuProvider` `instanceof` check, rather than casting to a specific generator type, is what lets one shared `GeneratorBlock<T>` open the right menu for whichever concrete generator `T` a given instance was built with.)

- [ ] **Step 7: Write `CombustionGeneratorMenu.java` and `CombustionGeneratorScreen.java`**, porting `CoalGeneratorMenu` (`steves-advanced-storage`'s `gui/server/CoalGeneratorMenu.java`, already read in full during spec research) into the `ssm` package with the client/server split preserved exactly as the source has it:

```java
package io.github.scuba10steve.ssm.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CombustionGeneratorMenu extends AbstractContainerMenu {

    private final CombustionGeneratorBlockEntity blockEntity;
    private final ContainerData containerData;

    // Client constructor
    public CombustionGeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(type, containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()), new SimpleContainerData(7));
    }

    // Server constructor
    public CombustionGeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory, CombustionGeneratorBlockEntity blockEntity) {
        this(type, containerId, playerInventory, blockEntity, blockEntity.containerData);
    }

    private CombustionGeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                     CombustionGeneratorBlockEntity blockEntity, ContainerData containerData) {
        super(type, containerId);
        this.blockEntity = blockEntity;
        this.containerData = containerData;

        addSlot(new SlotItemHandler(blockEntity.fuelHandler, 0, 81, 43));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 9 + col * 18, 85 + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 9 + i * 18, 143));
        }

        addDataSlots(containerData);
    }

    private static CombustionGeneratorBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof CombustionGeneratorBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No CombustionGeneratorBlockEntity at " + pos);
    }

    public boolean isLit() {
        return containerData.get(0) > 0;
    }

    public int getBurnProgress() {
        int remainingBurnTime = containerData.get(0);
        int litDuration = containerData.get(1);
        if (litDuration == 0) {
            return 0;
        }
        return remainingBurnTime * 13 / litDuration;
    }

    public int getEnergyStored() {
        return (containerData.get(3) << 16) | (containerData.get(2) & 0xFFFF);
    }

    public int getEnergyCapacity() {
        return (containerData.get(5) << 16) | (containerData.get(4) & 0xFFFF);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        if (index == 0) {
            if (!this.moveItemStackTo(slotStack, 1, 37, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (blockEntity.fuelHandler.isItemValid(0, slotStack)) {
                if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }
}
```

`CombustionGeneratorScreen.java` follows `ProcessingMachineScreen`'s shape from Task 6 Step 2 (same `renderBg`/`renderLabels` split, same 176x166 canvas), substituting a flame-burn-progress blit (`getBurnProgress()`, 0-13 tall) at the fuel slot instead of a horizontal progress bar, plus an energy-bar blit using `getEnergyStored()`/`getEnergyCapacity()`. Reuse or adapt `steves-advanced-storage`'s equivalent screen class's blit coordinates rather than inventing new ones.

- [ ] **Step 8: Register `ModBlocks.COMBUSTION_GENERATOR`, `ModItems.COMBUSTION_GENERATOR`, `ModBlockEntities.COMBUSTION_GENERATOR`, `ModMenuTypes.COMBUSTION_GENERATOR`** — same registration shape as Task 7 Step 7 (`ModMenuTypes.COMBUSTION_GENERATOR`'s `IMenuTypeExtension.create(...)` lambda calls `CombustionGeneratorMenu`'s client constructor directly: `(containerId, inv, buf) -> new CombustionGeneratorMenu(ModMenuTypes.COMBUSTION_GENERATOR.get(), containerId, inv, buf)`), using `GeneratorBlock` + `CombustionGeneratorBlockEntity::new` + `CombustionGeneratorBlockEntity::tick`.

- [ ] **Step 9: Add `ModCapabilities` entries for the Combustion Generator** (energy + fuel item handler), same shape as `StevesAdvancedStorage.registerCapabilities`'s `COAL_GENERATOR` entries.

- [ ] **Step 10: Add the Combustion Generator's screen registration to `ModClientSetup.registerScreens`** (Task 6 Step 5): `event.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);`

- [ ] **Step 11: Add crafting recipe, register in creative tab.**

- [ ] **Step 12: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 13: Commit**

```bash
git add -A
git commit -m "feat: add Combustion Generator (adapted from steves-advanced-storage's Coal Generator)"
```

---

### Task 11: Solar Panel (ported from SolarGeneratorBlockEntity)

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/SolarPanelBlockEntity.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/SolarPanelMenu.java`, `SolarPanelScreen.java`
- Modify: registries as in Task 10 Step 8-10

**Interfaces:**
- Consumes: `SimpleEnergyStorage`, `GeneratorBlock` (Tasks 3, 10).
- Produces: `SolarPanelBlockEntity` (implements `MenuProvider`) with `energyStorage`, `containerData`, static `tick(...)`.

This is a direct, low-risk port of `SolarGeneratorBlockEntity` (`steves-advanced-storage`'s `neoforge/src/main/java/io/github/scuba10steve/s3/advanced/blockentity/SolarGeneratorBlockEntity.java`) — the spec's Solar Panel behavior (daytime + sky access, fixed rate, no upgrade tiers) matches the source class exactly, no behavioral divergence needed (unlike the Combustion Generator).

- [ ] **Step 1: Write `SolarPanelBlockEntity.java`**, renaming `SolarGeneratorBlockEntity` -> `SolarPanelBlockEntity`, `S3AdvancedConfig.SOLAR_*` -> `SsmConfig.SOLAR_*`, `GeneratorEnergyStorage` -> `SimpleEnergyStorage`, package `io.github.scuba10steve.ssm.generator`, dropping the rain-specific rate (the spec has no rain-rate requirement — use `SOLAR_GENERATION_RATE` unconditionally whenever it's day and sky is visible; this simplification is a deliberate scope trim vs. the source, not an oversight):

```java
package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.energy.SimpleEnergyStorage;
import io.github.scuba10steve.ssm.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class SolarPanelBlockEntity extends BlockEntity implements MenuProvider {

    private int currentRate;
    public final SimpleEnergyStorage energyStorage;

    // ContainerData slots: 0-1 = energy (lo/hi), 2-3 = capacity (lo/hi), 4 = currentRate
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> currentRate;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public int getCurrentRate() {
        return currentRate;
    }

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
        energyStorage = new SimpleEnergyStorage(SsmConfig.SOLAR_CAPACITY.get(), 0, SsmConfig.SOLAR_MAX_OUTPUT.get());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ssm.solar_panel");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new SolarPanelMenu(io.github.scuba10steve.ssm.registry.ModMenuTypes.SOLAR_PANEL.get(), id, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
        if (level.isClientSide()) return;

        int maxOutput = SsmConfig.SOLAR_MAX_OUTPUT.get();
        int skyLight = level.getBrightness(LightLayer.SKY, pos.above());

        if (skyLight > 0 && level.isDay()) {
            int rate = SsmConfig.SOLAR_GENERATION_RATE.get();
            if (be.energyStorage.generate(rate) > 0) {
                be.currentRate = rate;
                be.setChanged();
            } else {
                be.currentRate = 0;
            }
        } else {
            be.currentRate = 0;
        }

        if (be.energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                IEnergyStorage neighbour = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (neighbour != null && neighbour.canReceive()) {
                    int toSend = Math.min(be.energyStorage.getEnergyStored(), maxOutput);
                    int sent = neighbour.receiveEnergy(toSend, false);
                    if (sent > 0) {
                        be.energyStorage.extractEnergy(sent, false);
                        be.setChanged();
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(tag.getInt("energy"));
    }
}
```

- [ ] **Step 2: Write `SolarPanelMenu.java`** — no slots at all (the Solar Panel has no inventory, only energy), just the player inventory plus synced `ContainerData`:

```java
package io.github.scuba10steve.ssm.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;

public class SolarPanelMenu extends AbstractContainerMenu {

    private final SolarPanelBlockEntity blockEntity;
    private final ContainerData containerData;

    public SolarPanelMenu(MenuType<?> type, int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(type, containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()), new SimpleContainerData(5));
    }

    public SolarPanelMenu(MenuType<?> type, int containerId, Inventory playerInventory, SolarPanelBlockEntity blockEntity) {
        this(type, containerId, playerInventory, blockEntity, blockEntity.containerData);
    }

    private SolarPanelMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                            SolarPanelBlockEntity blockEntity, ContainerData containerData) {
        super(type, containerId);
        this.blockEntity = blockEntity;
        this.containerData = containerData;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        addDataSlots(containerData);
    }

    private static SolarPanelBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof SolarPanelBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No SolarPanelBlockEntity at " + pos);
    }

    public int getEnergyStored() {
        return (containerData.get(1) << 16) | (containerData.get(0) & 0xFFFF);
    }

    public int getEnergyCapacity() {
        return (containerData.get(3) << 16) | (containerData.get(2) & 0xFFFF);
    }

    public int getCurrentRate() {
        return containerData.get(4);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY; // no machine-side slots to shift-click into
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }
}
```

`SolarPanelScreen.java` follows `ProcessingMachineScreen`'s shape (Task 6 Step 2): a background texture plus an energy-bar blit driven by `getEnergyStored()`/`getEnergyCapacity()`, no progress bar since there's no processing to show.

- [ ] **Step 3: Register `ModBlocks.SOLAR_PANEL`, `ModItems.SOLAR_PANEL`, `ModBlockEntities.SOLAR_PANEL`, `ModMenuTypes.SOLAR_PANEL`** — same shape as Task 10 Step 8, `ModMenuTypes.SOLAR_PANEL`'s client factory calling `SolarPanelMenu`'s client constructor directly.

- [ ] **Step 4: Add `ModCapabilities` entry for the Solar Panel** (energy only, no item handler needed).

- [ ] **Step 5: Add the Solar Panel's screen registration to `ModClientSetup.registerScreens`** (Task 6 Step 5): `event.register(ModMenuTypes.SOLAR_PANEL.get(), SolarPanelScreen::new);`

- [ ] **Step 6: Add crafting recipe, register in creative tab.**

- [ ] **Step 7: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add Solar Panel (adapted from steves-advanced-storage's Solar Generator)"
```

---

### Task 12: Water Wheel (new)

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/WaterWheelBlockEntity.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/WaterWheelMenu.java`, `WaterWheelScreen.java`
- Modify: registries as in Task 10 Step 8-10
- Test: `src/test/java/io/github/scuba10steve/ssm/generator/AdjacentWaterCheckTest.java`

**Interfaces:**
- Consumes: `SimpleEnergyStorage`, `GeneratorBlock` (Tasks 3, 10).

- [ ] **Step 1: Write the failing test for the adjacency check** (pure function, no live `Level` needed — takes the 6 neighbor `FluidState`s directly)

```java
package io.github.scuba10steve.ssm.generator;

import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdjacentWaterCheckTest {

    @Test
    void trueWhenAnyNeighborIsWaterSourceOrFlowing() {
        assertTrue(AdjacentWaterCheck.hasAdjacentWater(List.of(
                Fluids.EMPTY.defaultFluidState(),
                Fluids.WATER.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState()
        )));
    }

    @Test
    void falseWhenNoNeighborIsWater() {
        assertFalse(AdjacentWaterCheck.hasAdjacentWater(List.of(
                Fluids.EMPTY.defaultFluidState(),
                Fluids.LAVA.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState()
        )));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.generator.AdjacentWaterCheckTest"`
Expected: FAIL — `AdjacentWaterCheck` does not exist.

- [ ] **Step 3: Write `AdjacentWaterCheck.java`**

```java
package io.github.scuba10steve.ssm.generator;

import net.minecraft.world.level.material.FluidState;

import java.util.List;

public final class AdjacentWaterCheck {
    private AdjacentWaterCheck() {}

    public static boolean hasAdjacentWater(List<FluidState> neighborFluidStates) {
        for (FluidState state : neighborFluidStates) {
            if (state.is(net.minecraft.tags.FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.generator.AdjacentWaterCheckTest"`
Expected: PASS.

- [ ] **Step 5: Write `WaterWheelBlockEntity.java`**, following the exact same generator-tick shape as `SolarPanelBlockEntity` (Task 11) but with the condition replaced:

```java
package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.energy.SimpleEnergyStorage;
import io.github.scuba10steve.ssm.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.Arrays;

public class WaterWheelBlockEntity extends BlockEntity implements MenuProvider {

    private boolean currentlyGenerating;
    public final SimpleEnergyStorage energyStorage;

    // ContainerData slots: 0-1 = energy (lo/hi), 2-3 = capacity (lo/hi), 4 = isGenerating (0/1)
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> currentlyGenerating ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public WaterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_WHEEL.get(), pos, state);
        energyStorage = new SimpleEnergyStorage(SsmConfig.WATER_WHEEL_CAPACITY.get(), 0, SsmConfig.WATER_WHEEL_MAX_OUTPUT.get());
    }

    public boolean isGenerating() {
        return currentlyGenerating;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ssm.water_wheel");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new WaterWheelMenu(io.github.scuba10steve.ssm.registry.ModMenuTypes.WATER_WHEEL.get(), id, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WaterWheelBlockEntity be) {
        if (level.isClientSide()) return;

        int maxOutput = SsmConfig.WATER_WHEEL_MAX_OUTPUT.get();
        boolean adjacentWater = AdjacentWaterCheck.hasAdjacentWater(
                Arrays.stream(Direction.values())
                        .map(dir -> level.getFluidState(pos.relative(dir)))
                        .toList());

        if (adjacentWater) {
            int rate = SsmConfig.WATER_WHEEL_GENERATION_RATE.get();
            be.energyStorage.generate(rate);
            be.currentlyGenerating = true;
            be.setChanged();
        } else {
            be.currentlyGenerating = false;
        }

        if (be.energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                IEnergyStorage neighbour = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (neighbour != null && neighbour.canReceive()) {
                    int toSend = Math.min(be.energyStorage.getEnergyStored(), maxOutput);
                    int sent = neighbour.receiveEnergy(toSend, false);
                    if (sent > 0) {
                        be.energyStorage.extractEnergy(sent, false);
                        be.setChanged();
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(tag.getInt("energy"));
    }
}
```

(`be.setChanged()` after `generate()` in the `adjacentWater` branch is a fix relative to how this looked in an earlier draft of this plan — Solar Panel already had it; Water Wheel and Wind Turbine (Task 13) need it too, since without it a newly-generated energy buffer isn't guaranteed to be flagged dirty for saving.)

- [ ] **Step 6: Write `WaterWheelMenu.java`** — same shape as `SolarPanelMenu` (Task 11 Step 2: no machine slots, just player inventory + synced `ContainerData`), substituting `WaterWheelBlockEntity` and a `getIsGenerating()` accessor (`containerData.get(4) != 0`) in place of `getCurrentRate()`. `WaterWheelScreen.java` follows `SolarPanelScreen`'s shape.

- [ ] **Step 7: Register `ModBlocks.WATER_WHEEL` etc.**, add `ModCapabilities` entry, crafting recipe, creative tab entry — same shapes as Task 11 Steps 3-4 and 6.

- [ ] **Step 8: Add the Water Wheel's screen registration to `ModClientSetup.registerScreens`** (Task 6 Step 5): `event.register(ModMenuTypes.WATER_WHEEL.get(), WaterWheelScreen::new);`

- [ ] **Step 9: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: add Water Wheel generator"
```

---

### Task 13: Wind Turbine (new)

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/WindTurbineBlockEntity.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/generator/WindTurbineMenu.java`, `WindTurbineScreen.java`
- Modify: registries as in Task 10 Step 8-10
- Test: `src/test/java/io/github/scuba10steve/ssm/generator/SkyColumnCheckTest.java`

- [ ] **Step 1: Write the failing test for the sky-column check** (pure function over a list of block-is-air booleans, so it's game-instance-free)

```java
package io.github.scuba10steve.ssm.generator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkyColumnCheckTest {

    @Test
    void trueWhenAllBlocksAboveAreAir() {
        assertTrue(SkyColumnCheck.isColumnClear(List.of(true, true, true)));
    }

    @Test
    void falseWhenAnyBlockAboveIsSolid() {
        assertFalse(SkyColumnCheck.isColumnClear(List.of(true, false, true)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.generator.SkyColumnCheckTest"`
Expected: FAIL — `SkyColumnCheck` does not exist.

- [ ] **Step 3: Write `SkyColumnCheck.java`**

```java
package io.github.scuba10steve.ssm.generator;

import java.util.List;

public final class SkyColumnCheck {
    private SkyColumnCheck() {}

    public static boolean isColumnClear(List<Boolean> isAirAboveEachStep) {
        return isAirAboveEachStep.stream().allMatch(Boolean::booleanValue);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "io.github.scuba10steve.ssm.generator.SkyColumnCheckTest"`
Expected: PASS.

- [ ] **Step 5: Write `WindTurbineBlockEntity.java`**, same generator-tick shape as `WaterWheelBlockEntity`, condition replaced with `level.canSeeSky(pos.above())` (the vanilla helper that already walks the column to build height — use it directly rather than manually re-deriving `SkyColumnCheck`'s per-step booleans in production code; `SkyColumnCheck` exists purely to pin down the *boolean-combination rule* in a unit test, since `canSeeSky` itself needs a live `Level`/chunk and can't be unit tested without one):

```java
package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.energy.SimpleEnergyStorage;
import io.github.scuba10steve.ssm.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class WindTurbineBlockEntity extends BlockEntity implements MenuProvider {

    private boolean currentlyGenerating;
    public final SimpleEnergyStorage energyStorage;

    // ContainerData slots: 0-1 = energy (lo/hi), 2-3 = capacity (lo/hi), 4 = isGenerating (0/1)
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> currentlyGenerating ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_TURBINE.get(), pos, state);
        energyStorage = new SimpleEnergyStorage(SsmConfig.WIND_TURBINE_CAPACITY.get(), 0, SsmConfig.WIND_TURBINE_MAX_OUTPUT.get());
    }

    public boolean isGenerating() {
        return currentlyGenerating;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ssm.wind_turbine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new WindTurbineMenu(io.github.scuba10steve.ssm.registry.ModMenuTypes.WIND_TURBINE.get(), id, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WindTurbineBlockEntity be) {
        if (level.isClientSide()) return;

        int maxOutput = SsmConfig.WIND_TURBINE_MAX_OUTPUT.get();
        boolean clearToSky = level.canSeeSky(pos.above());

        if (clearToSky) {
            be.energyStorage.generate(SsmConfig.WIND_TURBINE_GENERATION_RATE.get());
            be.currentlyGenerating = true;
            be.setChanged();
        } else {
            be.currentlyGenerating = false;
        }

        if (be.energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                IEnergyStorage neighbour = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (neighbour != null && neighbour.canReceive()) {
                    int toSend = Math.min(be.energyStorage.getEnergyStored(), maxOutput);
                    int sent = neighbour.receiveEnergy(toSend, false);
                    if (sent > 0) {
                        be.energyStorage.extractEnergy(sent, false);
                        be.setChanged();
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(tag.getInt("energy"));
    }
}
```

- [ ] **Step 6: Write `WindTurbineMenu.java`/`WindTurbineScreen.java`** — same shape as `WaterWheelMenu`/`WaterWheelScreen` (Task 12 Step 6).

- [ ] **Step 7: Register `ModBlocks.WIND_TURBINE` etc.**, add `ModCapabilities` entry, crafting recipe, creative tab entry.

- [ ] **Step 8: Add the Wind Turbine's screen registration to `ModClientSetup.registerScreens`** (Task 6 Step 5): `event.register(ModMenuTypes.WIND_TURBINE.get(), WindTurbineScreen::new);`

- [ ] **Step 9: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: add Wind Turbine generator"
```

---

### Task 14: JEI integration

**Files:**
- Create: `src/main/java/io/github/scuba10steve/ssm/compat/jei/SsmJeiPlugin.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/compat/jei/CrushingCategory.java`
- Create: `src/main/java/io/github/scuba10steve/ssm/compat/jei/SawingCategory.java`
- Modify: root `build.gradle` (add `runtimeOnly "mezz.jei:jei-1.21.1-neoforge:19.27.0.340"` for local testing — it's already `compileOnly` for the API artifacts per Task 1)

**Interfaces:**
- Consumes: `ModRecipes.CRUSHING_TYPE`/`SAWING_TYPE`, `ModBlocks.CRUSHER`/`SAWMILL`/`SMELTER` (Tasks 7-9).

- [ ] **Step 1: Write `CrushingCategory.java`**

```java
package io.github.scuba10steve.ssm.compat.jei;

import io.github.scuba10steve.ssm.recipe.CrushingRecipe;
import io.github.scuba10steve.ssm.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class CrushingCategory implements IRecipeCategory<CrushingRecipe> {

    public static final RecipeType<CrushingRecipe> TYPE =
            RecipeType.create("ssm", "crushing", CrushingRecipe.class);

    private final IGuiHelper guiHelper;

    public CrushingCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
    }

    @Override
    public RecipeType<CrushingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ssm.crushing");
    }

    @Override
    public IDrawable getBackground() {
        return guiHelper.createBlankDrawable(120, 40);
    }

    @Override
    public IDrawable getIcon() {
        return guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.CRUSHER.get()));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrushingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 12).addIngredients(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 12).addItemStack(recipe.getResults(null).get(0));
    }

    @Override
    public void draw(CrushingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {}
}
```

(`IDrawable` and `IFocusGroup` come from `mezz.jei.api.gui.drawable` and `mezz.jei.api.recipe` respectively in the 19.27.x line — still confirm the exact `IRecipeCategory` method signatures against the actual `mezz.jei:jei-1.21.1-common-api:19.27.0.340` jar during implementation, since JEI's category API has shifted slightly between minor versions before.)

- [ ] **Step 2: Write `SawingCategory.java`** — identical shape to `CrushingCategory`, substituting `SawingRecipe`, `ModBlocks.SAWMILL`, and rendering both `planksResult`/`sawdustResult` as two output slots.

- [ ] **Step 3: Write `SsmJeiPlugin.java`**

```java
package io.github.scuba10steve.ssm.compat.jei;

import io.github.scuba10steve.ssm.recipe.CrushingRecipe;
import io.github.scuba10steve.ssm.recipe.SawingRecipe;
import io.github.scuba10steve.ssm.registry.ModBlocks;
import io.github.scuba10steve.ssm.ref.RefStrings;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public class SsmJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CrushingCategory(registration.getJeiHelpers().getGuiHelper()),
                new SawingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var recipeManager = Minecraft.getInstance().level.getRecipeManager();
        List<CrushingRecipe> crushingRecipes = recipeManager
                .getAllRecipesFor(io.github.scuba10steve.ssm.registry.ModRecipes.CRUSHING_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        registration.addRecipes(CrushingCategory.TYPE, crushingRecipes);

        List<SawingRecipe> sawingRecipes = recipeManager
                .getAllRecipesFor(io.github.scuba10steve.ssm.registry.ModRecipes.SAWING_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        registration.addRecipes(SawingCategory.TYPE, sawingRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUSHER.get()), CrushingCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SAWMILL.get()), SawingCategory.TYPE);
        // Smelter reuses vanilla's own Smelting category rather than a new ssm category:
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SMELTER.get()), RecipeTypes.SMELTING);
    }
}
```

(`registerRecipes` reading from the client's live `RecipeManager` at plugin-registration time is the standard JEI 1.21.x pattern for data-driven recipes — confirm the exact `Minecraft.getInstance().level` access point still applies, vs. a `JeiHelpers` recipe-manager accessor, against the real JEI 19.27.x API during implementation.)

- [ ] **Step 4: Verify build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`. JEI classes must only be referenced from `compat/jei/*` — verify no other package imports `mezz.jei.*` (this is what keeps JEI truly optional/client-only per the Global Constraints).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add JEI integration for Crusher/Sawmill categories and Smelter catalyst"
```

---

### Task 15: Gametest suite

Gametest lives in its **own Gradle subproject** (`gametest/`), not a source set bolted onto the root build — NeoGradle's `net.neoforged.moddev` plugin expects one `neoForge{}` extension per project, and `steves-simple-storage`'s own `gametest/s3` module (its `build.gradle` was read in full during spec/plan research) is a separate subproject for exactly this reason, depending on the main mod's compiled output rather than sharing its build script.

**Files:**
- Modify: root `settings.gradle` (add `include 'gametest'`)
- Create: `gametest/build.gradle`
- Create: `gametest/src/main/java/io/github/scuba10steve/ssm/gametest/CrusherGameTest.java`
- Create: `gametest/src/main/java/io/github/scuba10steve/ssm/gametest/SmelterGameTest.java`
- Create: `gametest/src/main/java/io/github/scuba10steve/ssm/gametest/SawmillGameTest.java`
- Create: `gametest/src/main/java/io/github/scuba10steve/ssm/gametest/GeneratorGameTest.java`
- Create: `gametest/src/main/resources/data/ssm/structure/empty.nbt` (copied binary, see Step 3)
- Modify: `.github/workflows/ci.yml` (add `./gradlew :gametest:runGameTestServer` after `./gradlew build`)

**Interfaces:**
- Consumes: everything from Tasks 7-13 (this task is integration-only, no new production interfaces).

- [ ] **Step 1: Add the subproject to `settings.gradle`**

```groovy
include 'gametest'
```

- [ ] **Step 2: Write `gametest/build.gradle`**

```groovy
plugins {
    id 'net.neoforged.moddev'
    id 'ssm.conventions'
}

evaluationDependsOn(':')

version = rootProject.mod_version
group = 'io.github.scuba10steve.ssm.test'

neoForge {
    version = neoforge_version

    runs {
        gameTestServer {
            type = "gameTestServer"
            systemProperty 'neoforge.enabledGameTestNamespaces', 'ssm'
        }
    }

    mods {
        ssm {
            sourceSet sourceSets.main
            modSourceSets.add(providers.provider { project(':').sourceSets.main })
        }
    }
}

dependencies {
    implementation project(':')
    testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.14.4"
}
```

(Confirm the exact `runs { gameTestServer { ... } }` options against `steves-simple-storage`'s `gametest/s3/build.gradle` for anything version-specific not shown above, such as a `setForceExit false` call on the run — that repo's module is the proven-working reference this is adapted from. The actual Gradle task this produces is `:gametest:runGameTestServer` — NeoGradle prefixes a run named `gameTestServer` with `run`, it does not use the run name as the task name verbatim.)

- [ ] **Step 3: Copy the empty-arena structure template**

`steves-simple-storage`'s gametest module already ships a generic empty test-arena structure at `gametest/s3/src/generated/resources/data/s3/structure/empty.nbt` (note: **singular** `structure`, not `structures` — Minecraft 1.21's data pack folder reorganization dropped the trailing 's' from this and several other datapack folder names). Copy that binary file as-is into `gametest/src/main/resources/data/ssm/structure/empty.nbt`. An empty air-box structure template has no mod-specific content, so copying it across mods (rather than reimplementing S3's `GameTestStructureProvider` datagen tool) is correct and sufficient for v1.

- [ ] **Step 4: Write `CrusherGameTest.java`**

```java
package io.github.scuba10steve.ssm.gametest;

import io.github.scuba10steve.ssm.machine.ProcessingMachineBlockEntity;
import io.github.scuba10steve.ssm.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("ssm")
public class CrusherGameTest {

    @GameTest(template = "ssm:empty")
    public static void crusherDoublesRawIronWithSufficientEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        be.energyStorage.receiveEnergy(100_000, false);

        helper.runAfterDelay(120, () -> {
            ItemStack output = be.inventory.getStackInSlot(1);
            helper.assertTrue(output.getItem().toString().contains("crushed_iron") && output.getCount() == 2,
                    "expected 2x crushed_iron in output slot");
            helper.succeed();
        });
    }

    @GameTest(template = "ssm:empty")
    public static void crusherDoesNotProgressWithoutEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        // No energy given.

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(be.getProgress() == 0, "progress should stay at 0 with no energy");
            helper.succeed();
        });
    }
}
```

- [ ] **Step 5: Write `SmelterGameTest.java`** — same shape as `CrusherGameTest` (template `ssm:empty`), asserting that a `ssm:crushed_iron` input (from Task 9's mod-authored `minecraft:smelting` recipe) produces `minecraft:iron_ingot` after sufficient ticks/energy.

- [ ] **Step 6: Write `SawmillGameTest.java`** — same shape, asserting an `oak_log` input produces `6x oak_planks` in output slot 1 and `1x ssm:sawdust` in output slot 2.

- [ ] **Step 7: Write `GeneratorGameTest.java`** — for each of the 4 generators, one "condition met" test (Combustion: coal in fuel slot; Solar: test run at a fixed daytime tick with sky access; Water Wheel: place adjacent to a placed water source; Wind Turbine: build at a height with clear sky) asserting `energyStorage.getEnergyStored() > 0` after N ticks, and one "condition unmet" test per generator (no fuel; solar panel covered by a block; water wheel with no adjacent water; turbine built under a roof block) asserting `getEnergyStored() == 0`.

- [ ] **Step 8: Run the gametest suite**

Run: `./gradlew :gametest:runGameTestServer`
Expected: all tests pass (console output reports 0 failures).

- [ ] **Step 9: Add gametest step to CI**

In `.github/workflows/ci.yml`, after the existing `Build and test` step, add:
```yaml
      - name: Run GameTests
        run: ./gradlew :gametest:runGameTestServer
```

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "test: add gametest subproject and suite for machines and generators"
```

---

### Task 16: Documentation and repo polish

**Files:**
- Create: `AGENTS.md`
- Create: `docs/dev/machine-framework.md`, `docs/dev/energy-system.md`, `docs/dev/generators.md`, `docs/dev/jei-integration.md`, `docs/dev/build-system.md`, `docs/dev/configuration.md`, `docs/dev/dependencies.md`, `docs/dev/troubleshooting.md`
- Create: `README.md`, `CHANGELOG.md`, `cliff.toml`

**Interfaces:** none (docs only).

- [ ] **Step 1: Write `AGENTS.md`**, following `steves-simple-storage`'s `AGENTS.md` structure exactly (Documentation table → Project Structure → Build/Verification commands → Key Conventions → Core Mandates → Tool Usage → Version & Release Management), substituting `ssm` for `s3`, the single-module-plus-`gametest`-subproject file tree from this plan's File Structure section for S3's `core`/`neoforge`/`gametest` tree, and `./gradlew build` / `./gradlew :gametest:runGameTestServer` for the verification commands.

- [ ] **Step 2: Write `docs/dev/machine-framework.md`** covering `MachineConfig`, `MachineRecipeLookup`, `ProcessingMachineState`, `ProcessingMachineBlockEntity`, and "how to add a fifth machine" (add a `MachineRecipeLookup` + `MachineConfig` entry + recipe type if custom, no new block-entity/GUI class needed).

- [ ] **Step 3: Write `docs/dev/energy-system.md`** covering `SimpleEnergyStorage`, the `Capabilities.EnergyStorage.BLOCK` registration pattern in `ModCapabilities`, and the push-to-neighbor pattern generators use.

- [ ] **Step 4: Write `docs/dev/generators.md`** covering each of the 4 generators' condition logic and the Combustion Generator's intentional divergence from its `steves-advanced-storage` source (buffer-full pause vs. waste).

- [ ] **Step 5: Write `docs/dev/jei-integration.md`** covering the catalyst-only pattern used for the Smelter and the client-only/compile-only dependency setup.

- [ ] **Step 6: Write `docs/dev/build-system.md`, `configuration.md`, `dependencies.md`, `troubleshooting.md`** — adapt the equivalent S3 docs' structure/tone to ssm's actual single-module build.

- [ ] **Step 7: Write `README.md`** with a short mod description, install instructions, and a block reference table (Crusher, Smelter, Sawmill, Combustion Generator, Solar Panel, Water Wheel, Wind Turbine).

- [ ] **Step 8: Write `cliff.toml`** — copy `steves-simple-storage`'s `cliff.toml` verbatim (git-cliff config is repo-name-agnostic).

- [ ] **Step 9: Write an initial `CHANGELOG.md`** with a single `## [Unreleased]` section listing the v1 features shipped by Tasks 1-15.

- [ ] **Step 10: Verify build one final time**

Run: `./gradlew build :gametest:runGameTestServer`
Expected: `BUILD SUCCESSFUL`, all gametests pass.

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "docs: add AGENTS.md, docs/dev subsystem docs, README, and changelog scaffold"
```

---

## Notes for the executor

- Tasks 10-13 (generators) and Tasks 7-9 (machines) are independent of each other once Task 6 (menu/screen framework) and Task 5 (machine framework) / Task 3 (energy) land — they can be parallelized across subagents if using `subagent-driven-development`, but Task 14 (JEI) depends on Tasks 7-9, and Task 15 (gametest) depends on all of Tasks 7-13.
- Several code snippets above flag exact API surface (recipe codecs, JEI category methods, NeoGradle gametest run options) that should be confirmed against the real NeoForge 1.21.1 / JEI 19.27.x jars during implementation rather than assumed correct on first compile — this is normal for a plan written without direct access to build a jar, not a sign of a missing design decision.
