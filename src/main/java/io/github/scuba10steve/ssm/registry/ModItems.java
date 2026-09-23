package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, RefStrings.MODID);

    public static final Supplier<Item> CRUSHED_IRON = ITEMS.register("crushed_iron", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CRUSHED_GOLD = ITEMS.register("crushed_gold", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CRUSHED_COPPER = ITEMS.register("crushed_copper", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> CRUSHER = ITEMS.register("crusher",
            () -> new BlockItem(ModBlocks.CRUSHER.get(), new Item.Properties()));

    public static final Supplier<Item> SAWDUST = ITEMS.register("sawdust", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> SAWMILL = ITEMS.register("sawmill",
            () -> new BlockItem(ModBlocks.SAWMILL.get(), new Item.Properties()));

    public static final Supplier<Item> SMELTER = ITEMS.register("smelter",
            () -> new BlockItem(ModBlocks.SMELTER.get(), new Item.Properties()));

    public static final Supplier<Item> COMBUSTION_GENERATOR = ITEMS.register("combustion_generator",
            () -> new BlockItem(ModBlocks.COMBUSTION_GENERATOR.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
