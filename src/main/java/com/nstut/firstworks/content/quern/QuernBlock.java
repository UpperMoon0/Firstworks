package com.nstut.firstworks.content.quern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.jetbrains.annotations.Nullable;

public final class QuernBlock extends BaseEntityBlock {
    public static final MapCodec<QuernBlock> CODEC = simpleCodec(QuernBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 6.5, 15),
            Block.box(2.5, 6.5, 2.5, 13.5, 9, 13.5),
            Block.box(7, 9, 7, 9, 12, 9));

    public QuernBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new QuernBlockEntity(pos, state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide) return null;
        return createTickerHelper(type, com.nstut.firstworks.registry.ModBlockEntities.QUERN.get(), QuernBlockEntity::clientTick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof QuernBlockEntity quern) || stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!quern.canInsert(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) quern.insert(stack, player.getAbilities().instabuild);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof QuernBlockEntity quern)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (quern.takeOutput(player)) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown() && quern.takeInput(player)) return InteractionResult.SUCCESS;
            if (quern.work()) return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof QuernBlockEntity quern) {
            Containers.dropItemStack(level, pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,quern.getInput());
            Containers.dropItemStack(level, pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,quern.getOutput());
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
