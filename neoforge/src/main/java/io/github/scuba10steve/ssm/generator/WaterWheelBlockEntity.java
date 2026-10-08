package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.registry.ModBlockEntities;
import io.github.scuba10steve.ssm.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class WaterWheelBlockEntity extends GeneratorBlockEntity {

    private boolean currentlyGenerating;
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

    public WaterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_WHEEL.get(), pos, state, SsmConfig.WATER_WHEEL_CAPACITY.get(), SsmConfig.WATER_WHEEL_MAX_OUTPUT.get());
    }

    public boolean isGenerating() {
        return currentlyGenerating;
    }

    public int getCurrentRate() {
        return currentRate;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ssm.water_wheel");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new WaterWheelMenu(ModMenuTypes.WATER_WHEEL.get(), id, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WaterWheelBlockEntity be) {
        if (level.isClientSide()) return;

        boolean adjacentWater = AdjacentWaterCheck.hasAdjacentWater(
                Arrays.stream(Direction.values())
                        .map(dir -> level.getFluidState(pos.relative(dir)))
                        .toList());

        be.currentlyGenerating = adjacentWater && be.generateAndMarkChanged(SsmConfig.WATER_WHEEL_GENERATION_RATE.get());
        be.currentRate = be.currentlyGenerating ? SsmConfig.WATER_WHEEL_GENERATION_RATE.get() : 0;

        be.pushEnergyToNeighbors(level, pos, SsmConfig.WATER_WHEEL_MAX_OUTPUT.get());
    }
}
