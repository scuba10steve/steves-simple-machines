package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, RefStrings.MODID);

    public static final Supplier<CreativeModeTab> SSM_TAB = TABS.register("ssm_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ssm"))
                    .icon(() -> new ItemStack(ModItems.CRUSHER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.CRUSHER.get());
                        output.accept(ModItems.CRUSHED_IRON.get());
                        output.accept(ModItems.CRUSHED_GOLD.get());
                        output.accept(ModItems.CRUSHED_COPPER.get());
                        output.accept(ModItems.SAWMILL.get());
                        output.accept(ModItems.SAWDUST.get());
                        output.accept(ModItems.SMELTER.get());
                        output.accept(ModItems.COMBUSTION_GENERATOR.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
