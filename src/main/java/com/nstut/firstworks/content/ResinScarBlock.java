package com.nstut.firstworks.content;

import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ResinScarBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    private static final VoxelShape NORTH_SHAPE = Block.box(4.0, 3.0, 0.0, 12.0, 13.0, 1.5);
    private static final VoxelShape SOUTH_SHAPE = Block.box(4.0, 3.0, 14.5, 12.0, 13.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0, 3.0, 4.0, 1.5, 13.0, 12.0);
    private static final VoxelShape EAST_SHAPE = Block.box(14.5, 3.0, 4.0, 16.0, 13.0, 12.0);

    public ResinScarBlock(Properties properties) {
        super(properties.randomTicks().noCollission());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).is(ModTags.RESIN_TREES);
    }

    public static boolean hasLivingSupport(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING));
        return ResinTreeSupport.isLivingTree(level, support);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING) && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos) || !hasLivingSupport(state, level, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        int age = state.getValue(AGE);
        if (age < 3 && random.nextInt(3) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < 3) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!hasLivingSupport(state, level, pos)) {
                level.removeBlock(pos, false);
                return InteractionResult.SUCCESS;
            }
            Block.popResource(level, pos, new ItemStack(ModItems.RESIN.get()));
            level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.55F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
