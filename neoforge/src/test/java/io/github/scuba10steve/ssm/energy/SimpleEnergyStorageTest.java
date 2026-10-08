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
    void consumeReducesStoredEnergyEvenWithZeroMaxExtract() {
        // maxExtract = 0 is how machines block external cables from pulling FE back out;
        // consume() must still be able to drain the buffer for the machine's own internal use.
        SimpleEnergyStorage storage = new SimpleEnergyStorage(1000, Integer.MAX_VALUE, 0);
        storage.setEnergy(700);

        int consumed = storage.consume(300);

        assertEquals(300, consumed);
        assertEquals(400, storage.getEnergyStored());
    }

    @Test
    void consumeClampsToAvailableEnergy() {
        SimpleEnergyStorage storage = new SimpleEnergyStorage(1000, Integer.MAX_VALUE, 0);
        storage.setEnergy(200);

        int consumed = storage.consume(300);

        assertEquals(200, consumed);
        assertEquals(0, storage.getEnergyStored());
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
