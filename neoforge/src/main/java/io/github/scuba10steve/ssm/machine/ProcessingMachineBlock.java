package io.github.scuba10steve.ssm.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
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

public class ProcessingMachineBlock extends BaseEntityBlock {

    private final BiFunction<BlockPos, BlockState, ProcessingMachineBlockEntity> factory;

    public ProcessingMachineBlock(BiFunction<BlockPos, BlockState, ProcessingMachineBlockEntity> factory, Properties properties) {
        super(properties);
        this.factory = factory;
    }

    // Required by BlockBehaviour since 1.20.5's block-codec system; this block is only ever
    // constructed directly by ModBlocks (never reconstructed from a codec), so this exists
    // purely to satisfy the abstract method contract.
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new ProcessingMachineBlock(factory, properties));
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
        if (level.getBlockEntity(pos) instanceof ProcessingMachineBlockEntity be) {
            player.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ProcessingMachineBlockEntity pmbe) {
                ProcessingMachineBlockEntity.tick(lvl, pos, st, pmbe);
            }
        };
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ProcessingMachineBlockEntity be) {
            dropInventory(level, pos, be.inventory);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void dropInventory(Level level, BlockPos pos, ItemStackHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
    }
}
