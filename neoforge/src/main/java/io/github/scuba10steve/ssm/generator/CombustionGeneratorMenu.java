package io.github.scuba10steve.ssm.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CombustionGeneratorMenu extends AbstractContainerMenu {

    private final CombustionGeneratorBlockEntity blockEntity;
    private final ContainerData containerData;

    // Client constructor
    public CombustionGeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(type, containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()), new SimpleContainerData(7));
    }

    // Server constructor
    public CombustionGeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory, CombustionGeneratorBlockEntity blockEntity) {
        this(type, containerId, playerInventory, blockEntity, blockEntity.containerData);
    }

    private CombustionGeneratorMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                     CombustionGeneratorBlockEntity blockEntity, ContainerData containerData) {
        super(type, containerId);
        this.blockEntity = blockEntity;
        this.containerData = containerData;

        addSlot(new SlotItemHandler(blockEntity.fuelHandler, 0, 81, 43));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 9 + col * 18, 85 + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 9 + i * 18, 143));
        }

        addDataSlots(containerData);
    }

    private static CombustionGeneratorBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof CombustionGeneratorBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No CombustionGeneratorBlockEntity at " + pos);
    }

    public boolean isLit() {
        return containerData.get(0) > 0;
    }

    public int getBurnProgress() {
        int remainingBurnTime = containerData.get(0);
        int litDuration = containerData.get(1);
        if (litDuration == 0) {
            return 0;
        }
        return remainingBurnTime * 13 / litDuration;
    }

    public int getEnergyStored() {
        return (containerData.get(3) << 16) | (containerData.get(2) & 0xFFFF);
    }

    public int getEnergyCapacity() {
        return (containerData.get(5) << 16) | (containerData.get(4) & 0xFFFF);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        if (index == 0) {
            if (!this.moveItemStackTo(slotStack, 1, 37, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (blockEntity.fuelHandler.isItemValid(0, slotStack)) {
                if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }
}
