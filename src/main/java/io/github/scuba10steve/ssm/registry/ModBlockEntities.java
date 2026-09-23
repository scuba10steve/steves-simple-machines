package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.generator.CombustionGeneratorBlockEntity;
import io.github.scuba10steve.ssm.generator.SolarPanelBlockEntity;
import io.github.scuba10steve.ssm.machine.MachineConfigs;
import io.github.scuba10steve.ssm.machine.ProcessingMachineBlockEntity;
import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RefStrings.MODID);

    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> CRUSHER =
            BLOCK_ENTITIES.register("crusher", () ->
                    BlockEntityType.Builder.of((pos, state) ->
                            new ProcessingMachineBlockEntity(ModBlockEntities.CRUSHER.get(), pos, state, MachineConfigs.CRUSHER),
                            ModBlocks.CRUSHER.get()).build(null));

    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> SAWMILL =
            BLOCK_ENTITIES.register("sawmill", () ->
                    BlockEntityType.Builder.of((pos, state) ->
                            new ProcessingMachineBlockEntity(ModBlockEntities.SAWMILL.get(), pos, state, MachineConfigs.SAWMILL),
                            ModBlocks.SAWMILL.get()).build(null));

    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> SMELTER =
            BLOCK_ENTITIES.register("smelter", () ->
                    BlockEntityType.Builder.of((pos, state) ->
                            new ProcessingMachineBlockEntity(ModBlockEntities.SMELTER.get(), pos, state, MachineConfigs.SMELTER),
                            ModBlocks.SMELTER.get()).build(null));

    public static final Supplier<BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITIES.register("combustion_generator", () ->
                    BlockEntityType.Builder.of(CombustionGeneratorBlockEntity::new,
                            ModBlocks.COMBUSTION_GENERATOR.get()).build(null));

    public static final Supplier<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITIES.register("solar_panel", () ->
                    BlockEntityType.Builder.of(SolarPanelBlockEntity::new,
                            ModBlocks.SOLAR_PANEL.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
