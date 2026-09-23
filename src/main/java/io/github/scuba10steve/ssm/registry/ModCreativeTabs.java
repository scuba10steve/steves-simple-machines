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
                    .icon(() -> ItemStack.EMPTY) // Task 7 replaces this with the Crusher item once registered
                    .displayItems((params, output) -> {
                        // Later tasks call output.accept(...) for each registered item.
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
