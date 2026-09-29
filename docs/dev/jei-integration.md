# JEI Integration

Just Enough Items (JEI) integration lives entirely under
`neoforge/src/main/java/io/github/scuba10steve/ssm/compat/jei/`: `SsmJeiPlugin`, `CrushingCategory`, `SawingCategory`.

## Two custom categories, one catalyst-only reuse

`SsmJeiPlugin` registers two brand-new recipe categories — `ssm:crushing` and `ssm:sawing` — because those recipe
types don't correspond to anything vanilla JEI already knows how to display:

```java
@Override
public void registerCategories(IRecipeCategoryRegistration registration) {
    registration.addRecipeCategories(
            new CrushingCategory(registration.getJeiHelpers().getGuiHelper()),
            new SawingCategory(registration.getJeiHelpers().getGuiHelper()));
}

@Override
public void registerRecipes(IRecipeRegistration registration) {
    RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
    registration.addRecipes(CrushingCategory.TYPE, recipeManager.getAllRecipesFor(ModRecipes.CRUSHING_TYPE.get())
            .stream().map(RecipeHolder::value).toList());
    registration.addRecipes(SawingCategory.TYPE, recipeManager.getAllRecipesFor(ModRecipes.SAWING_TYPE.get())
            .stream().map(RecipeHolder::value).toList());
}
```

Each category (`CrushingCategory`, `SawingCategory`) is a small `IRecipeCategory<T>` implementation: a 120x40 GUI
with an input slot and one (`CrushingCategory`) or two (`SawingCategory`, for planks + sawdust) output slots, an icon
drawn from the machine's own `ItemStack`, and a translatable title (`jei.ssm.crushing` / `jei.ssm.sawing`).

**The Smelter gets no new category.** Smelting recipes already have a perfectly good JEI category — vanilla's own
`RecipeTypes.SMELTING`. Since the Smelter block just runs vanilla `minecraft:smelting` recipes (see
[machine-framework.md](./machine-framework.md)), duplicating that as a third `ssm` category would
show every smeltable item twice in JEI for no benefit. Instead, the Smelter is registered purely as a **catalyst** —
"this block is a valid place to use this vanilla category's recipes" — without touching `registerCategories` or
`registerRecipes` for smelting at all:

```java
@Override
public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
    registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUSHER.get()), CrushingCategory.TYPE);
    registration.addRecipeCatalyst(new ItemStack(ModBlocks.SAWMILL.get()), SawingCategory.TYPE);
    // Smelter reuses vanilla's own Smelting category rather than a new ssm category:
    registration.addRecipeCatalyst(new ItemStack(ModBlocks.SMELTER.get()), RecipeTypes.SMELTING);
}
```

The practical effect: hovering a smelting recipe in JEI shows both the vanilla Furnace **and** the Smelter as valid
places to make it, with zero duplicate recipe entries. The Crusher and Sawmill are also registered as catalysts for
their own new categories, in addition to having those categories and recipes registered above — a catalyst
registration only says "recipes of this category can be made here," it doesn't populate the category itself.

**When adding a 5th machine:** if it reuses an existing recipe type (vanilla or `ssm`), follow the Smelter's
catalyst-only pattern — don't create a redundant category. Only add a new `IRecipeCategory` if the machine introduces
a genuinely new recipe shape that no existing JEI category can render.

## Client-only / compile-only dependency setup

JEI is a **client-only, optional** dependency: the mod must compile and run correctly (client and dedicated server)
whether or not JEI is installed alongside it. This is enforced entirely through dependency scoping, not runtime
`ModList.get().isLoaded(...)` checks:

```groovy
// buildSrc/src/main/groovy/ssm.neoforge-mod.gradle (applied by neoforge/ and gametest/, not core/)
dependencies {
    compileOnly "mezz.jei:jei-1.21.1-common-api:19.27.0.340"
    compileOnly "mezz.jei:jei-1.21.1-neoforge-api:19.27.0.340"
    ...
}

// neoforge/build.gradle
dependencies {
    runtimeOnly "mezz.jei:jei-1.21.1-neoforge:19.27.0.340"
}
```

- **`compileOnly`** on the JEI API jars: `SsmJeiPlugin` and the two categories can reference JEI's interfaces
  (`IModPlugin`, `IRecipeCategory`, etc.) at compile time, but those jars are **not** bundled into the built mod jar
  and are **not** required at runtime just to load `ssm`. This is what makes JEI truly optional — a server or client
  without JEI installed still loads `ssm` fine; `SsmJeiPlugin` (annotated `@JeiPlugin`) simply never gets instantiated
  because JEI itself isn't present to discover it. Since `ssm.neoforge-mod` is shared by both `neoforge/` and
  `gametest/`, `gametest/` also gets the JEI API on its compile classpath, though it has no JEI-referencing code of
  its own.
- **`runtimeOnly`** on the full JEI mod jar, declared only in `neoforge/build.gradle` (not in the shared
  `ssm.neoforge-mod` convention plugin, and not in `gametest/build.gradle`): this makes JEI present in the dev
  environment (`runClient`, and anywhere `neoforge`'s runtime classpath is used) so the integration can actually be
  exercised during development, without making JEI a compile-time or `neoforge.mods.toml` dependency of the shipped
  mod. It is deliberately **not** added to `gametest/`'s dependencies — the gametest suite exercises machine/generator
  gameplay via `NeoForge.EVENT_BUS`/`GameTestHelper`, not JEI's own GUI, so it doesn't need JEI on its classpath at
  runtime.

If you add JEI API usage anywhere outside `compat/jei/`, keep it behind the same `compileOnly` boundary — anything
that unconditionally imports a JEI class at runtime (outside a class JEI itself instantiates via `@JeiPlugin`) would
break the "JEI is optional" contract.
