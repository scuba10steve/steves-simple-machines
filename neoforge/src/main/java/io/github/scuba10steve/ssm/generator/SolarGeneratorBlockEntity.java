package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.registry.ModBlockEntities;
import io.github.scuba10steve.ssm.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class SolarGeneratorBlockEntity extends GeneratorBlockEntity {

    private int currentRate;

    // ContainerData slots: 0-1 = energy (lo/hi), 2-3 = capacity (lo/hi), 4 = currentRate
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyLo();
                case 1 -> energyHi();
                case 2 -> capacityLo();
                case 3 -> capacityHi();
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

    public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_GENERATOR.get(), pos, state, SsmConfig.SOLAR_CAPACITY.get(), SsmConfig.SOLAR_MAX_OUTPUT.get());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ssm.solar_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new SolarGeneratorMenu(ModMenuTypes.SOLAR_GENERATOR.get(), id, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarGeneratorBlockEntity be) {
        if (level.isClientSide()) return;

        int skyLight = level.getBrightness(LightLayer.SKY, pos.above());
        boolean canGenerate = skyLight > 0 && level.isDay();
        boolean generated = canGenerate && be.generateAndMarkChanged(SsmConfig.SOLAR_GENERATION_RATE.get());
        be.currentRate = generated ? SsmConfig.SOLAR_GENERATION_RATE.get() : 0;

        be.pushEnergyToNeighbors(level, pos, SsmConfig.SOLAR_MAX_OUTPUT.get());
    }
}
