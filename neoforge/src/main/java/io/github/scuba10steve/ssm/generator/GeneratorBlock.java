package io.github.scuba10steve.ssm.generator;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.BiFunction;
import java.util.function.Function;

public class GeneratorBlock<T extends BlockEntity> extends BaseEntityBlock {

    private final BiFunction<BlockPos, BlockState, T> factory;
    private final GeneratorTicker<T> ticker;
    // Nullable: only the Combustion Generator has an inventory (its fuel slot) to drop on removal.
    // Solar Panel, Water Wheel and Wind Turbine have no ItemStackHandler and pass null.
    private final Function<T, ItemStackHandler> inventoryAccessor;

    public interface GeneratorTicker<T extends BlockEntity> {
        void tick(Level level, BlockPos pos, BlockState state, T blockEntity);
    }

    public GeneratorBlock(BiFunction<BlockPos, BlockState, T> factory, GeneratorTicker<T> ticker, Properties properties) {
        this(factory, ticker, null, properties);
    }

    public GeneratorBlock(BiFunction<BlockPos, BlockState, T> factory, GeneratorTicker<T> ticker,
                           Function<T, ItemStackHandler> inventoryAccessor, Properties properties) {
        super(properties);
        this.factory = factory;
        this.ticker = ticker;
        this.inventoryAccessor = inventoryAccessor;
    }

    // Required by BlockBehaviour since 1.20.5's block-codec system; this block is only ever
    // constructed directly by ModBlocks (never reconstructed from a codec), so this exists
    // purely to satisfy the abstract method contract. (Same rationale as ProcessingMachineBlock.)
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new GeneratorBlock<>(factory, ticker, inventoryAccessor, properties));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(Level level, BlockState state, BlockEntityType<U> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, be) -> ticker.tick(lvl, pos, st, (T) be);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity be = level.getBlockEntity(pos);
        if (inventoryAccessor != null && !state.is(newState.getBlock()) && be != null) {
            ItemStackHandler inventory = inventoryAccessor.apply((T) be);
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
