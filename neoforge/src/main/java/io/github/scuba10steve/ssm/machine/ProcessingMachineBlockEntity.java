package io.github.scuba10steve.ssm.machine;

import io.github.scuba10steve.ssm.energy.SimpleEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ProcessingMachineBlockEntity extends BlockEntity implements MenuProvider {

    public final MachineConfig config;
    public final ItemStackHandler inventory;
    public final SimpleEnergyStorage energyStorage;
    private final ProcessingMachineState state;

    // ContainerData slots: 0 = progress, 1 = processTicks, 2-3 = energy (lo/hi), 4-5 = capacity (lo/hi)
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getProgress();
                case 1 -> config.baseProcessTicks().getAsInt();
                case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-authoritative fields only; server drives all of these, nothing to set.
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, MachineConfig config) {
        super(type, pos, blockState);
        this.config = config;
        // No isItemValid override here: ItemStackHandler.insertItem consults isItemValid
        // internally regardless of caller, so overriding it to reject output slots would
        // also block the machine's OWN completion logic from ever inserting its results.
        // Player-facing "outputs aren't inputs" is enforced instead at the Slot layer, in
        // ProcessingMachineMenu (Task 6), via SlotItemHandler#mayPlace.
        this.inventory = new ItemStackHandler(1 + config.outputSlotCount()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.energyStorage = new SimpleEnergyStorage(config.energyCapacity().getAsInt(), Integer.MAX_VALUE, 0);
        this.state = new ProcessingMachineState(new ItemStackHandlerInventory(inventory), config.outputSlotCount(), config.recipeLookup());
    }

    public int getProgress() {
        return state.getProgress();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ssm." + config.id());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ProcessingMachineMenu(config.menuType().get(), id, playerInventory, this);
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, ProcessingMachineBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        int perTick = be.config.energyPerTick().getAsInt();
        boolean hasEnergy = be.energyStorage.getEnergyStored() >= perTick;
        ProcessingMachineState.TickOutcome outcome = be.state.tick(level, hasEnergy);
        if (outcome.energyConsumed()) {
            be.energyStorage.consume(perTick);
        }
        be.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("progress", getProgress());
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(tag.getInt("energy"));
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        // ItemStackHandler#deserializeNBT resizes the handler to whatever "Size" was saved,
        // which corrupts blocks placed under an older MachineConfig with a different
        // outputSlotCount (e.g. the Crusher gained a 2nd output slot after some blocks were
        // already placed): ProcessingMachineMenu then adds a Slot for an index the resized
        // handler no longer has, crashing every time the menu opens. Re-assert the current
        // config's size afterward so stale saved data can never leave the handler undersized.
        // ItemStackHandler#setSize does NOT preserve contents (it replaces the whole stacks
        // list with fresh empties), so the already-deserialized stacks must be captured first
        // and restored after resizing, or every load would silently wipe the inventory.
        int expectedSize = 1 + config.outputSlotCount();
        if (inventory.getSlots() != expectedSize) {
            int preserveCount = Math.min(inventory.getSlots(), expectedSize);
            ItemStack[] existing = new ItemStack[preserveCount];
            for (int i = 0; i < preserveCount; i++) {
                existing[i] = inventory.getStackInSlot(i);
            }
            inventory.setSize(expectedSize);
            for (int i = 0; i < preserveCount; i++) {
                inventory.setStackInSlot(i, existing[i]);
            }
        }
        state.setProgress(tag.getInt("progress"));
    }
}
