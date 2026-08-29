package com.nstut.firstworks.content.charcoal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CharcoalPileBlock extends Block {
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, 4);

    protected static final VoxelShape SHAPE_1 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);
    protected static final VoxelShape SHAPE_2 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    protected static final VoxelShape SHAPE_3 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    protected static final VoxelShape SHAPE_4 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public CharcoalPileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AMOUNT, 1));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AMOUNT)) {
            case 1 -> SHAPE_1;
            case 2 -> SHAPE_2;
            case 3 -> SHAPE_3;
            default -> SHAPE_4;
        };
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.isSecondaryUseActive() && context.getItemInHand().is(this.asItem()) && state.getValue(AMOUNT) < 4) {
            return true;
        }
        return super.canBeReplaced(state, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState current = context.getLevel().getBlockState(context.getClickedPos());
        if (current.is(this)) {
            return current.setValue(AMOUNT, Math.min(4, current.getValue(AMOUNT) + 1));
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.CHARCOAL) && state.getValue(AMOUNT) < 4) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(AMOUNT, state.getValue(AMOUNT) + 1), 3);
                if (!player.hasInfiniteMaterials()) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return canSupportRigidBlock(level, below) || canSupportCenter(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AMOUNT);
    }
}
