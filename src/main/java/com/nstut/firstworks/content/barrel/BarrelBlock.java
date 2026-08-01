package com.nstut.firstworks.content.barrel;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModFluids;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.fluids.FluidStack;

public class BarrelBlock extends BaseEntityBlock {
    public static final MapCodec<BarrelBlock> CODEC = simpleCodec(BarrelBlock::new);
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape OPEN_SHAPE = Shapes.or(
            box(1, 0, 1, 15, 2, 15),
            box(1, 2, 1, 15, 14, 3),
            box(1, 2, 13, 15, 14, 15),
            box(1, 2, 3, 3, 14, 13),
            box(13, 2, 3, 15, 14, 13)
    ).optimize();
    private static final VoxelShape SEALED_SHAPE = Shapes.or(
            OPEN_SHAPE,
            box(2, 14, 2, 14, 15, 14),
            box(1, 15, 1, 15, 16, 3),
            box(1, 15, 13, 15, 16, 15),
            box(1, 15, 3, 3, 16, 13),
            box(13, 15, 3, 15, 16, 13),
            box(3, 15, 7, 13, 16, 9),
            box(6, 16, 6, 10, 17, 10),
            box(5, 17, 5, 11, 18, 11)
    ).optimize();

    public BarrelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SEALED, false).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(SEALED, POWERED);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.getBlock() != state.getBlock() && level instanceof ServerLevel serverLevel) {
            checkRedstonePulse(state, serverLevel, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel) {
            checkRedstonePulse(state, serverLevel, pos);
        }
    }

    private void checkRedstonePulse(BlockState state, ServerLevel level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;

        BlockState updated = state.setValue(POWERED, powered);
        if (powered) {
            boolean sealed = !state.getValue(SEALED);
            updated = updated.setValue(SEALED, sealed);
            if (!sealed && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                barrel.cancelProcess();
            }
            level.playSound(null, pos, sealed ? SoundEvents.WOODEN_TRAPDOOR_CLOSE : SoundEvents.WOODEN_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        level.setBlock(pos, updated, 3);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SEALED) ? SEALED_SHAPE : OPEN_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SEALED) ? SEALED_SHAPE : OPEN_SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrelBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.BARREL.get()
                ? (tickerLevel, pos, tickerState, entity) -> BarrelBlockEntity.tick(tickerLevel, pos, tickerState,
                        (BarrelBlockEntity) entity)
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(SEALED)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (stack.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide && barrel.addWater(1000)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(ModItems.TANNIN_SOLUTION_BUCKET.get())) {
            if (!level.isClientSide && barrel.addFluid(new FluidStack(ModFluids.TANNIN_SOLUTION.get(), 1_000))) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(Items.BUCKET)) {
            if (!level.isClientSide) {
                ItemStack filledBucket = barrel.drainBucket();
                if (!filledBucket.isEmpty()) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(filledBucket);
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(Items.POTION) && potion != null && potion.is(Potions.WATER) && !potion.hasEffects()) {
            if (!level.isClientSide && barrel.addWater(250)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (barrel.canInsert(stack)) {
            if (!level.isClientSide && barrel.insertIngredient(stack, player.getAbilities().instabuild)) {
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && barrel.takeOutput(player)) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            boolean sealed = !state.getValue(SEALED);
            level.setBlock(pos, state.setValue(SEALED, sealed), 3);
            if (!sealed) barrel.cancelProcess();
            level.playSound(null, pos, sealed ? SoundEvents.WOODEN_TRAPDOOR_CLOSE : SoundEvents.WOODEN_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, barrel.getIngredient());
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, barrel.getOutput());
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
