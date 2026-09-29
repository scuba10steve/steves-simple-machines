package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.registry.ModBlockEntities;
import io.github.scuba10steve.ssm.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CombustionGeneratorBlockEntity extends GeneratorBlockEntity {

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

    // ContainerData slots: 0 = remainingBurnTime, 1 = litDuration, 2-3 = energy (lo/hi), 4-5 = capacity (lo/hi), 6 = generationRate
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getRemainingBurnTime();
                case 1 -> litDuration;
                case 2 -> energyLo();
                case 3 -> energyHi();
                case 4 -> capacityLo();
                case 5 -> capacityHi();
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
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state, SsmConfig.COMBUSTION_CAPACITY.get(), SsmConfig.COMBUSTION_MAX_OUTPUT.get());
    }

    public boolean isLit() {
        return burnState.isBurning();
    }

    public int getRemainingBurnTime() {
        return burnState.getRemainingBurnTime();
    }

    public static int getBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return stack.getBurnTime(null);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CombustionGeneratorBlockEntity be) {
        if (level.isClientSide()) return;

        int capacity = SsmConfig.COMBUSTION_CAPACITY.get();
        int maxOutput = SsmConfig.COMBUSTION_MAX_OUTPUT.get();
        int generationRate = SsmConfig.COMBUSTION_GENERATION_RATE.get();
        int bufferRoom = capacity - be.energyStorage.getEnergyStored();

        boolean wasBurning = be.burnState.isBurning();

        if (be.burnState.isBurning()) {
            var result = be.burnState.tick(generationRate, bufferRoom);
            be.generateAndMarkChanged(result.energyToGenerate());
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

        be.pushEnergyToNeighbors(level, pos, maxOutput);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ssm.combustion_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(ModMenuTypes.COMBUSTION_GENERATOR.get(), id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("remainingBurnTime", burnState.getRemainingBurnTime());
        tag.putInt("litDuration", litDuration);
        tag.put("fuel", fuelHandler.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnState.setRemainingBurnTime(tag.getInt("remainingBurnTime"));
        litDuration = tag.getInt("litDuration");
        fuelHandler.deserializeNBT(registries, tag.getCompound("fuel"));
    }
}
