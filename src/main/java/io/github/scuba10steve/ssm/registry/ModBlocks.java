package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.generator.CombustionGeneratorBlockEntity;
import io.github.scuba10steve.ssm.generator.GeneratorBlock;
import io.github.scuba10steve.ssm.generator.SolarPanelBlockEntity;
import io.github.scuba10steve.ssm.machine.MachineConfigs;
import io.github.scuba10steve.ssm.machine.ProcessingMachineBlock;
import io.github.scuba10steve.ssm.machine.ProcessingMachineBlockEntity;
import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, RefStrings.MODID);

    public static final Supplier<ProcessingMachineBlock> CRUSHER = BLOCKS.register("crusher", () ->
            new ProcessingMachineBlock(
                    (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.CRUSHER.get(), pos, state, MachineConfigs.CRUSHER),
                    BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));

    public static final Supplier<ProcessingMachineBlock> SAWMILL = BLOCKS.register("sawmill", () ->
            new ProcessingMachineBlock(
                    (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.SAWMILL.get(), pos, state, MachineConfigs.SAWMILL),
                    BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));

    public static final Supplier<ProcessingMachineBlock> SMELTER = BLOCKS.register("smelter", () ->
            new ProcessingMachineBlock(
                    (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.SMELTER.get(), pos, state, MachineConfigs.SMELTER),
                    BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));

    public static final Supplier<GeneratorBlock<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR = BLOCKS.register("combustion_generator", () ->
            new GeneratorBlock<>(
                    CombustionGeneratorBlockEntity::new,
                    CombustionGeneratorBlockEntity::tick,
                    BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));

    public static final Supplier<GeneratorBlock<SolarPanelBlockEntity>> SOLAR_PANEL = BLOCKS.register("solar_panel", () ->
            new GeneratorBlock<>(
                    SolarPanelBlockEntity::new,
                    SolarPanelBlockEntity::tick,
                    BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
