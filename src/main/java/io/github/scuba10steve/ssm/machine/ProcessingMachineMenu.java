package io.github.scuba10steve.ssm.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ProcessingMachineMenu extends AbstractContainerMenu {

    private final ProcessingMachineBlockEntity blockEntity;
    private final ContainerData containerData;
    private final int outputSlotCount;

    // Client constructor (invoked via IMenuTypeExtension when the server tells the client to open this menu)
    public ProcessingMachineMenu(MenuType<?> type, int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(type, containerId, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()), new SimpleContainerData(6));
    }

    // Server constructor (invoked from ProcessingMachineBlockEntity#createMenu)
    public ProcessingMachineMenu(MenuType<?> type, int containerId, Inventory playerInventory, ProcessingMachineBlockEntity blockEntity) {
        this(type, containerId, playerInventory, blockEntity, blockEntity.containerData);
    }

    private ProcessingMachineMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                   ProcessingMachineBlockEntity blockEntity, ContainerData containerData) {
        super(type, containerId);
        this.blockEntity = blockEntity;
        this.containerData = containerData;
        this.outputSlotCount = blockEntity.config.outputSlotCount();

        addSlot(new SlotItemHandler(blockEntity.inventory, 0, 56, 35)); // input slot

        int outputStartX = 116;
        for (int i = 0; i < outputSlotCount; i++) {
            addSlot(new SlotItemHandler(blockEntity.inventory, 1 + i, outputStartX + (i * 18), 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false; // players can take output, but not insert into it directly
                }
            });
        }

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

    private static ProcessingMachineBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof ProcessingMachineBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No ProcessingMachineBlockEntity at " + pos);
    }

    // These read through containerData (synced), never through blockEntity's live fields directly — see Interfaces note above.
    public int getProgress() {
        return containerData.get(0);
    }

    public int getProcessTicks() {
        return containerData.get(1);
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
        int firstOutputIndex = 1;
        int lastOutputIndex = outputSlotCount; // inclusive
        int playerInvStart = outputSlotCount + 1;
        int playerInvEnd = playerInvStart + 36;

        if (index == 0 || (index >= firstOutputIndex && index <= lastOutputIndex)) {
            if (!this.moveItemStackTo(slotStack, playerInvStart, playerInvEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
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
