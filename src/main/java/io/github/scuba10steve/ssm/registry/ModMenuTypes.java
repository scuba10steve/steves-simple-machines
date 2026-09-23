package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.machine.ProcessingMachineMenu;
import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, RefStrings.MODID);

    public static final Supplier<MenuType<ProcessingMachineMenu>> CRUSHER = MENU_TYPES.register("crusher", () ->
            net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    (containerId, inv, buf) -> new ProcessingMachineMenu(ModMenuTypes.CRUSHER.get(), containerId, inv, buf)));

    public static final Supplier<MenuType<ProcessingMachineMenu>> SAWMILL = MENU_TYPES.register("sawmill", () ->
            net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    (containerId, inv, buf) -> new ProcessingMachineMenu(ModMenuTypes.SAWMILL.get(), containerId, inv, buf)));

    public static final Supplier<MenuType<ProcessingMachineMenu>> SMELTER = MENU_TYPES.register("smelter", () ->
            net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    (containerId, inv, buf) -> new ProcessingMachineMenu(ModMenuTypes.SMELTER.get(), containerId, inv, buf)));

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }
}
