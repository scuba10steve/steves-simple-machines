# Machine Framework

All three processing machines (Crusher, Smelter, Sawmill) share a single generic implementation. There is exactly one
`BlockEntity` class, one `Menu` class, and one `Screen` class for all of them — what makes each machine distinct is
a `MachineConfig` value, not a subclass.

## `MachineConfig`

`io.github.scuba10steve.ssm.machine.MachineConfig` is a record describing everything a machine needs that varies
per-machine: `id`, `recipeLookup`, `outputSlotCount`, `energyCapacity`/`energyPerTick`/`baseProcessTicks`, and
`menuType`.

- `id` — used for the translation key (`block.ssm.<id>`) and matches the machine's registry name.
- `recipeLookup` — how this machine finds a recipe for its current input (see below).
- `outputSlotCount` — 1 for Smelter, 2 for Crusher and Sawmill. Crusher's second slot is overflow
  buffer capacity, not a forced split: `CrushingRecipe` still produces one combined stack (e.g. 2x
  crushed ore), which lands entirely in the first output slot with room; the second slot only
  starts filling once the first reaches max stack size (see `ProcessingMachineState#insertAll`).
  Sawmill's two slots hold genuinely distinct items (planks, sawdust) instead.
- `energyCapacity` / `energyPerTick` / `baseProcessTicks` — `IntSupplier`s rather than plain `int`s so they read
  live `SsmConfig` values (config changes don't require re-registering the block).
- `menuType` — the machine's own registered `MenuType`, needed by `ProcessingMachineBlockEntity#createMenu`.

The three concrete instances live in `MachineConfigs` (`CRUSHER`, `SMELTER`, `SAWMILL`). Each one wires its
`recipeLookup` to `SsmConfig`'s per-machine capacity/energy/ticks fields.

## `MachineRecipeLookup` and `MachineRecipeResult`

`MachineRecipeLookup` is a single-method interface — `find(Level, ItemStack) -> Optional<MachineRecipeResult>` —
and `MachineRecipeResult` a record pairing a `List<ItemStack> outputs` with a `processTicks` count. This is the seam between "generic processing machine" and "how this specific machine decides what to make." A
lookup is free to:

- Query a custom `ssm` recipe type, as the Crusher and Sawmill do (`ModRecipes.CRUSHING_TYPE` /
  `ModRecipes.SAWING_TYPE`), returning every output the recipe produces (`SsmMachineRecipe#getResults`) and either
  the recipe's own tick override or the machine's configured `baseProcessTicks`.
- Reuse an existing vanilla recipe type, as the Smelter does. `MachineConfigs.SMELTER`'s lookup calls
  `level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, ...)`, assembles the vanilla result, and adapts it
  through `SmelterAdapter.toMachineResult(...)` into a single-output `MachineRecipeResult` with a **flat**
  `SMELTER_PROCESS_TICKS` (vanilla `cookingTime` is intentionally ignored — every smelting recipe takes the same
  configured number of ticks in this machine, for a consistent FE-per-item cost across all smeltables).

`SmelterAdapter` is a pure, stateless adapter (`vanilla ItemStack result -> MachineRecipeResult`), kept separate so
it's trivial to unit test without a `Level` (see `SmeltingLookupTest`).

## `ProcessingMachineState`

`ProcessingMachineState` is the actual state machine — progress tracking, recipe re-checking, and output insertion —
extracted out of the `BlockEntity` so it can be unit tested without spinning up a `Level` (`ProcessingMachineStateTest`
drives it directly).

`tick(Level, boolean energyAvailableForOneTick)` returns a `TickOutcome(boolean completed, boolean energyConsumed)`:

1. Look up a recipe for slot 0's current input via the configured `MachineRecipeLookup`. No recipe -> progress resets
   to 0, nothing consumed.
2. If progress hasn't reached the recipe's `processTicks`, advance it by one **only if** energy was available; if not,
   the tick is a no-op (progress doesn't fall behind, but it also doesn't advance for free).
3. Once progress reaches `processTicks`, the machine tries to insert every recipe output into the output slots. If it
   fits, insertion happens, the input is consumed, and progress resets to 0. If it doesn't fit, the machine is
   **parked at completion** — progress stays exactly at `processTicks` and no further energy is charged each tick
   spent waiting for space (`energyConsumed` is only ever true on the one tick that actually incremented progress).

`canInsertAll` simulates the whole output batch against a scratch copy of the output slots before touching real
inventory, so e.g. the Sawmill's two outputs (planks + sawdust) are only inserted if **both** fit together — a
partial insert that then discards the other output is never possible.

`ProcessingMachineState` lives in `core` and depends only on the small `MachineInventory` interface (3 methods:
`getStackInSlot`, `extractItem`, `insertItem`) rather than NeoForge's `ItemStackHandler` directly — this is what lets
the state machine (and its test) live in the platform-agnostic module. `neoforge`'s `ItemStackHandlerInventory` is
the adapter that wraps a real `ItemStackHandler` to implement `MachineInventory`; `ProcessingMachineBlockEntity`
constructs one and passes it to `ProcessingMachineState`'s constructor (see below).

## `ProcessingMachineBlockEntity`

A thin wrapper: owns the `ItemStackHandler` inventory (`1 + outputSlotCount` slots — slot 0 is input), the
`SimpleEnergyStorage`, wraps `inventory` in an `ItemStackHandlerInventory` when constructing `ProcessingMachineState`,
and delegates all state-machine logic to it. `tick(...)` (called by
`ProcessingMachineBlock#getTicker`) checks whether `energyPerTick` is available, calls
`state.tick(level, hasEnergy)`, and only then extracts the energy — the `TickOutcome.energyConsumed()` flag is what
actually gates the `extractEnergy` call, not a second energy check.

`ContainerData` (6 ints: progress, `baseProcessTicks`, energy stored lo/hi, capacity lo/hi) is exposed for the shared
`ProcessingMachineScreen` to render a progress arrow and energy bar without needing per-machine screen classes.

## Adding a fifth machine

Because the framework already carries all per-machine variation in `MachineConfig`, adding a new processing machine
does **not** need a new `BlockEntity`, `Menu`, or `Screen` class. It needs:

1. A `MachineRecipeLookup` — either a lambda querying a new custom recipe type, or one reusing an existing vanilla
   recipe type (following the Smelter's pattern via an adapter like `SmelterAdapter`).
2. A recipe type, **only if** no existing recipe type (vanilla or `ssm`) fits: a record implementing
   `SsmMachineRecipe` (see `CrushingRecipe`/`SawingRecipe`) plus a `RecipeType`/`RecipeSerializer` pair registered in
   `ModRecipes`.
3. A new `MachineConfig` entry in `MachineConfigs`, plus matching capacity/energy-per-tick/process-ticks fields in
   `SsmConfig`.
4. New registry entries reusing the existing classes: a `BlockEntityType<ProcessingMachineBlockEntity>` in
   `ModBlockEntities`, a `MenuType<ProcessingMachineMenu>` in `ModMenuTypes`, and a `ProcessingMachineBlock` /
   `BlockItem` pair in `ModBlocks` / `ModItems`. Add capability registrations for the new `BlockEntityType` in
   `ModCapabilities` (energy storage + item handler, mirroring the existing three).
5. Data: a crafting recipe JSON under `data/ssm/recipe/crafting/`, and recipe JSON(s) for the new recipe type if one
   was added in step 2.

No changes to `ProcessingMachineState`, `ProcessingMachineBlockEntity`, `ProcessingMachineBlock`,
`ProcessingMachineMenu`, or `ProcessingMachineScreen` are required.
