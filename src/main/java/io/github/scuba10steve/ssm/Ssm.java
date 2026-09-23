package io.github.scuba10steve.ssm;

import io.github.scuba10steve.ssm.ref.RefStrings;
import io.github.scuba10steve.ssm.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(RefStrings.MODID)
public class Ssm {
    public Ssm(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
    }
}
