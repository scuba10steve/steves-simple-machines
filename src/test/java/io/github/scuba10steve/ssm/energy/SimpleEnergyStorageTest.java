package io.github.scuba10steve.ssm.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleEnergyStorageTest {

    @Test
    void generateClampsAtCapacity() {
        SimpleEnergyStorage storage = new SimpleEnergyStorage(1000, 0, 500);
        int accepted = storage.generate(1500);
        assertEquals(1000, accepted);
        assertEquals(1000, storage.getEnergyStored());
    }

    @Test
    void generateAfterPartialFillOnlyAcceptsRemainingRoom() {
        SimpleEnergyStorage storage = new SimpleEnergyStorage(1000, 0, 500);
        storage.generate(700);
        int accepted = storage.generate(700);
        assertEquals(300, accepted);
        assertEquals(1000, storage.getEnergyStored());
    }

    @Test
    void setEnergyClampsToCapacityAndZero() {
        SimpleEnergyStorage storage = new SimpleEnergyStorage(1000, 0, 500);
        storage.setEnergy(5000);
        assertEquals(1000, storage.getEnergyStored());
        storage.setEnergy(-50);
        assertEquals(0, storage.getEnergyStored());
    }
}
