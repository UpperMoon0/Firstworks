package com.nstut.firstworks.content;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Hand-operated bellows. The nozzle must physically face an adjacent Crucible Furnace. */
public final class BellowsBlock extends BaseEntityBlock {
    public static final MapCodec<BellowsBlock> CODEC = simpleCodec(BellowsBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BellowsBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BellowsBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BellowsBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        BlockPos furnacePos = pos.relative(facing);
        if (!(level.getBlockEntity(furnacePos) instanceof WorkshopBlockEntity workshop)
                || !WorkshopRecipe.CRUCIBLE_FURNACE.equals(workshop.station())) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && workshop.stoke(160)) {
            if (level.getBlockEntity(pos) instanceof BellowsBlockEntity bellows) bellows.press();
            level.playSound(null, pos, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.BLOCKS, 0.62F, 0.72F);
            if (level instanceof ServerLevel server) {
                double x = pos.getX() + 0.5 + facing.getStepX() * 0.48;
                double y = pos.getY() + 0.34;
                double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.48;
                server.sendParticles(ParticleTypes.CLOUD, x, y, z, 6, 0.045, 0.025, 0.045, 0.018);
                server.sendParticles(ParticleTypes.SMALL_FLAME,
                        furnacePos.getX() + 0.5, furnacePos.getY() + 0.42, furnacePos.getZ() + 0.5,
                        3, 0.10, 0.07, 0.10, 0.01);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
