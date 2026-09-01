package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public abstract class WorkshopBlock extends BaseEntityBlock {
    private final String station;

    protected WorkshopBlock(Properties properties, String station) {
        super(properties);
        this.station = station;
    }

    public String station() { return station; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WorkshopBlockEntity(pos, state); }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.WORKSHOP.get(), WorkshopBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WorkshopBlockEntity workshop) || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (station.equals(WorkshopRecipe.STONE_ANVIL) && stack.is(ModTags.HAMMERS)) {
            if (!level.isClientSide && workshop.work(player)) {
                EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                if (!player.hasInfiniteMaterials()) stack.hurtAndBreak(1, player, slot);
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.65F, 1.35F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!workshop.canInsert(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) workshop.insert(stack, player.hasInfiniteMaterials());
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WorkshopBlockEntity workshop)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (workshop.takeOutput(player)) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) return workshop.takeStored(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            if (station.equals(WorkshopRecipe.POTTERY_WHEEL)) workshop.work(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof WorkshopBlockEntity workshop) {
            for (ItemStack stack : workshop.allStacks()) {
                if (!stack.isEmpty()) Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack);
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
