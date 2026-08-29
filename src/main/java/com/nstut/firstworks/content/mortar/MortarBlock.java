package com.nstut.firstworks.content.mortar;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class MortarBlock extends BaseEntityBlock {
    public static final MapCodec<MortarBlock> CODEC = simpleCodec(MortarBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.or(
            box(4, 0, 4, 12, 2, 12),
            box(4, 2, 3, 12, 6, 5),
            box(4, 2, 11, 12, 6, 13),
            box(3, 2, 5, 5, 6, 11),
            box(11, 2, 5, 13, 6, 11)).optimize();

    public MortarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MortarBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return type == ModBlockEntities.MORTAR.get()
                ? (tickerLevel, tickerPos, tickerState, entity) -> MortarBlockEntity.tick(
                        tickerLevel, tickerPos, tickerState, (MortarBlockEntity) entity)
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof MortarBlockEntity mortar) || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!mortar.canInsert(stack)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && mortar.insert(stack, player.getAbilities().instabuild)) {
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.55F, 1.15F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof MortarBlockEntity mortar)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (mortar.takeOutput(player)) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.55F, 1.15F);
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown() && mortar.takeInput(player)) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            ItemStack material = mortar.getInput().copyWithCount(1);
            if (!player.isShiftKeyDown() && mortar.startGrinding()) {
                level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.55F, 0.8F);
                if (level instanceof ServerLevel serverLevel && !material.isEmpty()) {
                    serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, material),
                            pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5,
                            5, 0.10, 0.03, 0.10, 0.012);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof MortarBlockEntity mortar) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, mortar.getInput());
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, mortar.getOutput());
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
