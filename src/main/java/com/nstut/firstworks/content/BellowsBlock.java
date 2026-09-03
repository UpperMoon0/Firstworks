package com.nstut.firstworks.content;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.content.workshop.WorkshopBlock;
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
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Hand-operated bellows. The nozzle must physically face an adjacent Crucible Furnace. */
public final class BellowsBlock extends BaseEntityBlock {
    public static final MapCodec<BellowsBlock> CODEC = simpleCodec(BellowsBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int AIR_PER_PRESS = 160;
    private static final int MAX_AIR_RESERVE = AIR_PER_PRESS * 3;

    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(1.5, 0.0, 2.5, 14.5, 3.45, 14.0),
            Block.box(2.3, 3.0, 3.6, 13.7, 6.9, 12.4),
            Block.box(2.0, 6.5, 3.0, 14.0, 9.7, 14.6),
            Block.box(7.0, 1.35, 0.0, 9.0, 3.2, 4.2)
    ).optimize();
    private static final Map<Direction, VoxelShape> SHAPES = WorkshopBlock.makeHorizontalShapes(NORTH_SHAPE);

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
        // FACING is the physical nozzle direction, unlike front-facing workstations that face the player.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BellowsBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        BlockPos furnacePos = pos.relative(facing);
        if (!(level.getBlockEntity(furnacePos) instanceof WorkshopBlockEntity workshop)
                || !WorkshopRecipe.CRUCIBLE_FURNACE.equals(workshop.station())) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            int currentAir = workshop.getStokeTicks();
            if (currentAir < MAX_AIR_RESERVE) {
                int targetAir = Math.min(MAX_AIR_RESERVE, currentAir + AIR_PER_PRESS);
                if (workshop.stoke(targetAir)) {
                    if (level.getBlockEntity(pos) instanceof BellowsBlockEntity bellows) {
                        bellows.press();
                    }
                    int pressureStage = Math.max(1, (targetAir + AIR_PER_PRESS - 1) / AIR_PER_PRESS);
                    level.playSound(null, pos, SoundEvents.WIND_CHARGE_BURST.value(),
                            SoundSource.BLOCKS, 0.58F + pressureStage * 0.03F, 0.70F + pressureStage * 0.05F);
                    if (level instanceof ServerLevel server) {
                        double x = pos.getX() + 0.5 + facing.getStepX() * 0.48;
                        double y = pos.getY() + 0.34;
                        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.48;
                        server.sendParticles(ParticleTypes.CLOUD, x, y, z,
                                4 + pressureStage, 0.045, 0.025, 0.045, 0.018);
                        server.sendParticles(ParticleTypes.SMALL_FLAME,
                                furnacePos.getX() + 0.5, furnacePos.getY() + 0.42, furnacePos.getZ() + 0.5,
                                2 + pressureStage, 0.10, 0.07, 0.10, 0.01);
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
