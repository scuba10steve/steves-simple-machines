package io.github.scuba10steve.ssm.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class WindTurbineMenu extends AbstractContainerMenu {

    private final WindTurbineBlockEntity blockEntity;
    private final ContainerData containerData;

    // Client constructor
    public WindTurbineMenu(MenuType<?> type, int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(type, containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()), new SimpleContainerData(5));
    }

    // Server constructor
    public WindTurbineMenu(MenuType<?> type, int containerId, Inventory playerInventory, WindTurbineBlockEntity blockEntity) {
        this(type, containerId, playerInventory, blockEntity, blockEntity.containerData);
    }

    private WindTurbineMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                             WindTurbineBlockEntity blockEntity, ContainerData containerData) {
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

    private static WindTurbineBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof WindTurbineBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No WindTurbineBlockEntity at " + pos);
    }

    public int getEnergyStored() {
        return (containerData.get(1) << 16) | (containerData.get(0) & 0xFFFF);
    }

    public int getEnergyCapacity() {
        return (containerData.get(3) << 16) | (containerData.get(2) & 0xFFFF);
    }

    public boolean getIsGenerating() {
        return containerData.get(4) != 0;
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
