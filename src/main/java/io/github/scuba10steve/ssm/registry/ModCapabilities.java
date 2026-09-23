package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCapabilities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Tasks 5, 10, 11, 12, 13 each add one or more
        // event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, <TYPE>, (be, side) -> be.energyStorage);
        // lines here once their block entity types exist.
    }
}
