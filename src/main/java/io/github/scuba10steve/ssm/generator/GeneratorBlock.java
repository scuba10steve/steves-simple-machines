package io.github.scuba10steve.ssm.generator;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiFunction;

public class GeneratorBlock<T extends BlockEntity> extends BaseEntityBlock {

    private final BiFunction<BlockPos, BlockState, T> factory;
    private final GeneratorTicker<T> ticker;

    public interface GeneratorTicker<T extends BlockEntity> {
        void tick(Level level, BlockPos pos, BlockState state, T blockEntity);
    }

    public GeneratorBlock(BiFunction<BlockPos, BlockState, T> factory, GeneratorTicker<T> ticker, Properties properties) {
        super(properties);
        this.factory = factory;
        this.ticker = ticker;
    }

    // Required by BlockBehaviour since 1.20.5's block-codec system; this block is only ever
    // constructed directly by ModBlocks (never reconstructed from a codec), so this exists
    // purely to satisfy the abstract method contract. (Same rationale as ProcessingMachineBlock.)
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new GeneratorBlock<>(factory, ticker, properties));
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
}
