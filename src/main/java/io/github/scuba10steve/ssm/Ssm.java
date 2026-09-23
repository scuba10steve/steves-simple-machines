package io.github.scuba10steve.ssm;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.ref.RefStrings;
import io.github.scuba10steve.ssm.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(RefStrings.MODID)
public class Ssm {
    public Ssm(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, SsmConfig.SPEC, "ssm-common.toml");
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModCapabilities.register(modEventBus);
    }
}
