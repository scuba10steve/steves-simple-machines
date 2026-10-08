# Dependency Management

## Current Dependencies

Last checked: 2026-09-23

### Core Dependencies

| Dependency        | Current Version | Notes                                          |
|--------------------|-----------------|--------------------------------------------------|
| **NeoForge**       | 21.1.226        | Set in `gradle.properties` (`neoforge_version`)   |
| **Minecraft**      | 1.21.1          | Target version                                    |
| **ModDevGradle**   | 2.0.141         | `net.neoforged:moddev-gradle`, in `buildSrc`      |
| **Gradle**         | 9.5.1           | Via wrapper                                       |
| **Java**           | 21              | Required for NeoForge 1.21.1                      |

### Runtime Dependencies

| Dependency                  | Current Version | Notes                                                           |
|------------------------------|-----------------|-------------------------------------------------------------------|
| **JEI (Just Enough Items)**  | 19.27.0.340     | `compileOnly` API + `runtimeOnly` mod jar — see `dependencies.md` |

### Test Dependencies

| Dependency                    | Current Version | Notes                                     |
|--------------------------------|-----------------|---------------------------------------------|
| **JUnit Jupiter**              | 5.14.4          | `testImplementation`, declared per-module in `core/build.gradle` and `buildSrc/src/main/groovy/ssm.neoforge-mod.gradle` (`ssm.conventions.gradle` itself only sets up `useJUnitPlatform()`) |
| **JUnit Platform Launcher**    | 1.14.4 (`gametest`, pinned) / transitive (`core`; `neoforge` via `ssm.neoforge-mod`) | Runtime only |

## Version Compatibility

### NeoForge Version Matrix

| Minecraft | NeoForge Range | Current  |
|-----------|-----------------|----------|
| 1.21.1    | 21.1.x          | 21.1.226 |

`neoforge_version_range=[21.1,22)` in `gradle.properties` is what's actually shipped in `neoforge.mods.toml`
(via `processResources`' token substitution), separate from the exact build version pulled at compile time.

### JEI Compatibility

JEI 19.27.0.340 targets NeoForge 1.21.1. Both the API artifacts (`jei-1.21.1-common-api`, `jei-1.21.1-neoforge-api`)
and the runtime mod jar (`jei-1.21.1-neoforge`) must stay on matching versions.

`jei_version_range=[19.27.0,20)` in `gradle.properties` is what's shipped in `neoforge.mods.toml` as an
`optional`-typed dependency on `jei` (via `processResources`' token substitution, same mechanism as
`neoforge_version_range` above) — separate from the exact build version pulled at compile/runtime. Being
`optional` (not `required`) means `ssm` still loads fine with no JEI installed at all; the version range only
matters if JEI *is* present, so an incompatible version fails cleanly at NeoForge's own dependency-resolution
step instead of crashing during JEI's plugin discovery.

## Maven Repositories

Declared in `buildSrc/src/main/groovy/ssm.repositories.gradle`:

| Repository      | URL                                       | Used for                         |
|------------------|--------------------------------------------|-----------------------------------|
| Maven Central    | (default)                                  | JUnit                             |
| NeoForge         | `https://maven.neoforged.net/releases`     | NeoForge, ModDevGradle             |
| Mojang           | `https://libraries.minecraft.net`          | Minecraft/Mojang libraries         |
| Jared            | `https://maven.blamejared.com`             | JEI                                 |
| Modrinth         | `https://api.modrinth.com/maven`           | (available; not currently used by any declared dependency) |

## Checking for Updates

**NeoForge versions:**

```bash
curl -s "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml" | grep -E "<version>21\.1\." | tail -5
```

**JEI versions:** [Modrinth JEI page](https://modrinth.com/mod/jei/versions), filtered to NeoForge 1.21.1.

**JUnit versions:** [Maven Central](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter).

## Update Strategy

1. **Commit all changes first** — updates should be their own commit.
2. **Review changelogs** for breaking changes, especially for NeoForge/ModDevGradle.
3. **Rebuild and test:** `./gradlew build :gametest:runGameTestServer` after bumping any version in
   `gradle.properties`, `buildSrc/build.gradle`, `buildSrc/src/main/groovy/ssm.neoforge-mod.gradle` (JEI's
   `compileOnly` API deps), or `neoforge/build.gradle` (JEI's `runtimeOnly` mod jar) — JEI's version string is
   currently duplicated across those two Gradle files rather than centralized in `gradle.properties`, so keep both
   in sync manually.

## Update History

| Date       | Component | From | To | Notes |
|------------|-----------|------|----|-------|
| 2026-09-23 | Initial release | - | 0.1.0 | Tasks 1-15: machines, generators, JEI integration, gametest suite |

## References

- [NeoForge Releases](https://neoforged.net/categories/releases/)
- [NeoForge Maven Repository](https://maven.neoforged.net/releases/net/neoforged/neoforge/)
- [JEI on Modrinth](https://modrinth.com/mod/jei)
- [ModDevGradle Documentation](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)
