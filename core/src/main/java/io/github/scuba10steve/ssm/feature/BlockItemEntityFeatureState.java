package io.github.scuba10steve.ssm.feature;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A developer-controlled feature-readiness flag bundling a block/item/block-entity's registry
 * identity with its current enabled state. Registration (DeferredRegister) always happens
 * regardless of this flag; only discoverability (creative tab, JEI) and craftability (a recipe
 * condition) are gated by it.
 */
public final class BlockItemEntityFeatureState {

    private static final Map<ResourceLocation, BlockItemEntityFeatureState> BY_ITEM_ID = new HashMap<>();

    private final ResourceLocation id;
    private final boolean enabled;
    private final Supplier<? extends Item> item;
    private final Supplier<? extends Block> block;
    private final Supplier<? extends BlockEntityType<?>> blockEntityType;

    private BlockItemEntityFeatureState(ResourceLocation id, boolean enabled,
            Supplier<? extends Item> item, Supplier<? extends Block> block,
            Supplier<? extends BlockEntityType<?>> blockEntityType) {
        this.id = id;
        this.enabled = enabled;
        this.item = item;
        this.block = block;
        this.blockEntityType = blockEntityType;
        if (BY_ITEM_ID.putIfAbsent(id, this) != null) {
            throw new IllegalStateException("Duplicate BlockItemEntityFeatureState for " + id);
        }
    }

    public static BlockItemEntityFeatureState of(String namespace, String path, boolean enabled,
            Supplier<? extends Item> item, Supplier<? extends Block> block,
            Supplier<? extends BlockEntityType<?>> blockEntityType) {
        return new BlockItemEntityFeatureState(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                enabled, item, block, blockEntityType);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Item item() {
        return item.get();
    }

    public Block block() {
        return block != null ? block.get() : null;
    }

    public BlockEntityType<?> blockEntityType() {
        return blockEntityType != null ? blockEntityType.get() : null;
    }

    public static boolean isEnabled(ResourceLocation itemId) {
        BlockItemEntityFeatureState state = BY_ITEM_ID.get(itemId);
        return state == null || state.isEnabled();
    }

    public static Collection<BlockItemEntityFeatureState> all() {
        return BY_ITEM_ID.values();
    }
}
