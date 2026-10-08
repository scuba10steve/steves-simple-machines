package io.github.scuba10steve.ssm.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class SolarGeneratorMenu extends AbstractContainerMenu {

    private final SolarGeneratorBlockEntity blockEntity;
    private final ContainerData containerData;

    // Client constructor
    public SolarGeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(type, containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()), new SimpleContainerData(5));
    }

    // Server constructor
    public SolarGeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory, SolarGeneratorBlockEntity blockEntity) {
        this(type, containerId, playerInventory, blockEntity, blockEntity.containerData);
    }

    private SolarGeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                            SolarGeneratorBlockEntity blockEntity, ContainerData containerData) {
        super(type, containerId);
        this.blockEntity = blockEntity;
        this.containerData = containerData;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        addDataSlots(containerData);
    }

    private static SolarGeneratorBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof SolarGeneratorBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No SolarGeneratorBlockEntity at " + pos);
    }

    public int getEnergyStored() {
        return (containerData.get(1) << 16) | (containerData.get(0) & 0xFFFF);
    }

    public int getEnergyCapacity() {
        return (containerData.get(3) << 16) | (containerData.get(2) & 0xFFFF);
    }

    public int getCurrentRate() {
        return containerData.get(4);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no machine-side slots to shift-click into
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }
}
