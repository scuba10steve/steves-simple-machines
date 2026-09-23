package io.github.scuba10steve.ssm.energy;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.energy.EnergyStorage;

/** FE buffer shared by machines (as a sink) and generators (as a source). */
public class SimpleEnergyStorage extends EnergyStorage {

    public SimpleEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    /** Adds generated energy directly to the buffer, bypassing maxReceive. Returns the amount actually stored. */
    public int generate(int amount) {
        int accepted = Math.min(amount, capacity - energy);
        energy += accepted;
        return accepted;
    }

    public void setEnergy(int amount) {
        this.energy = Mth.clamp(amount, 0, capacity);
    }
}
