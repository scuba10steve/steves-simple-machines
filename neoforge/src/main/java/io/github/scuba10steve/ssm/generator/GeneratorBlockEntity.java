package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.energy.SimpleEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Shared base for all 4 generators: owns the FE buffer, the push-to-neighbor loop, the energy
 * NBT round-trip, and the lo/hi ContainerData bit-packing. Also owns the one invariant that two
 * of the four generators previously got wrong independently of each other: a generator's
 * "am I currently generating" state (and setChanged()) must be driven by whether energy was
 * ACTUALLY stored this tick, never by whether its environmental condition was merely met.
 */
public abstract class GeneratorBlockEntity extends BlockEntity implements MenuProvider {

    public final SimpleEnergyStorage energyStorage;

    protected GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxOutput) {
        super(type, pos, state);
        this.energyStorage = new SimpleEnergyStorage(capacity, 0, maxOutput);
    }

    /**
     * Generates up to `amount` FE into the buffer. Returns true only if energy was actually
     * stored (false if the buffer was already full or amount <= 0), and calls setChanged() only
     * in that case. Callers must drive their own "is generating" state from this return value.
     */
    protected final boolean generateAndMarkChanged(int amount) {
        if (amount <= 0) {
            return false;
        }
        int accepted = energyStorage.generate(amount);
        if (accepted > 0) {
            setChanged();
            return true;
        }
        return false;
    }

    /** Pushes stored FE to any neighboring block exposing Capabilities.EnergyStorage.BLOCK. */
    protected final void pushEnergyToNeighbors(Level level, BlockPos pos, int maxOutput) {
        if (energyStorage.getEnergyStored() <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            IEnergyStorage neighbour = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (neighbour != null && neighbour.canReceive()) {
                int toSend = Math.min(energyStorage.getEnergyStored(), maxOutput);
                int sent = neighbour.receiveEnergy(toSend, false);
                if (sent > 0) {
                    energyStorage.extractEnergy(sent, false);
                    setChanged();
                }
            }
        }
    }

    protected final int energyLo() { return energyStorage.getEnergyStored() & 0xFFFF; }
    protected final int energyHi() { return (energyStorage.getEnergyStored() >> 16) & 0xFFFF; }
    protected final int capacityLo() { return energyStorage.getMaxEnergyStored() & 0xFFFF; }
    protected final int capacityHi() { return (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF; }

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
