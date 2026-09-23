package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.generator.CombustionGeneratorScreen;
import io.github.scuba10steve.ssm.machine.ProcessingMachineScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// Note: RegisterMenuScreensEvent only fires client-side, and net.neoforged.fml.DistExecutor
// does not exist in NeoForge (it was a Forge-only class) — confirmed absent from the
// NeoForge 1.21.1 (21.1.226) dependency jars. No Dist gating is needed around the listener
// registration itself: the event class is part of the shared neoforge jar (loadable on both
// distributions), and this method's body (which references client-only screen classes)
// never executes on a dedicated server because the event is never fired there.
public class ModClientSetup {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModClientSetup::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.CRUSHER.get(), ProcessingMachineScreen::new);
        event.register(ModMenuTypes.SAWMILL.get(), ProcessingMachineScreen::new);
        event.register(ModMenuTypes.SMELTER.get(), ProcessingMachineScreen::new);
        event.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        // Tasks 11-13 each add their own generator MenuType/Screen pair.
    }
}
