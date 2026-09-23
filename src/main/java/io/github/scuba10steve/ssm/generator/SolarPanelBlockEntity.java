package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.energy.SimpleEnergyStorage;
import io.github.scuba10steve.ssm.registry.ModBlockEntities;
import io.github.scuba10steve.ssm.registry.ModMenuTypes;
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
        return new SolarPanelMenu(ModMenuTypes.SOLAR_PANEL.get(), id, playerInventory, this);
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
