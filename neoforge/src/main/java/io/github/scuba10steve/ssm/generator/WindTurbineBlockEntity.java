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

public class WindTurbineBlockEntity extends GeneratorBlockEntity {

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

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_TURBINE.get(), pos, state, SsmConfig.WIND_TURBINE_CAPACITY.get(), SsmConfig.WIND_TURBINE_MAX_OUTPUT.get());
    }

    public boolean isGenerating() {
        return currentlyGenerating;
    }

    public int getCurrentRate() {
        return currentRate;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ssm.wind_turbine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new WindTurbineMenu(ModMenuTypes.WIND_TURBINE.get(), id, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WindTurbineBlockEntity be) {
        if (level.isClientSide()) return;

        boolean exposed = WindExposureCheck.isExposed(
                pos.getY(),
                SsmConfig.WIND_TURBINE_MIN_HEIGHT.get(),
                Arrays.stream(Direction.values())
                        .map(dir -> level.getBlockState(pos.relative(dir)))
                        .toList(),
                SsmConfig.WIND_TURBINE_MIN_FREE_NEIGHBORS.get());

        be.currentlyGenerating = exposed && be.generateAndMarkChanged(SsmConfig.WIND_TURBINE_GENERATION_RATE.get());
        be.currentRate = be.currentlyGenerating ? SsmConfig.WIND_TURBINE_GENERATION_RATE.get() : 0;

        be.pushEnergyToNeighbors(level, pos, SsmConfig.WIND_TURBINE_MAX_OUTPUT.get());
    }
}
