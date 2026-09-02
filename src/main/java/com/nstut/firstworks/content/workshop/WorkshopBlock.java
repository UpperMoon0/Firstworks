package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Common in-world interaction contract for the Stone/Copper workshop stations.
 * There is deliberately no machine GUI: orientation, stored workpieces, heat and motion are rendered in-world.
 */
public abstract class WorkshopBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final double POTTERY_INNER_RADIUS_SQ = 0.18D * 0.18D;
    private static final double POTTERY_MIDDLE_RADIUS_SQ = 0.36D * 0.36D;

    private final String station;

    protected WorkshopBlock(Properties properties, String station) {
        super(properties);
        this.station = station;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public String station() {
        return station;
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
        return new WorkshopBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? createTickerHelper(type, ModBlockEntities.WORKSHOP.get(), WorkshopBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.WORKSHOP.get(), WorkshopBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof WorkshopBlockEntity workshop)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        if (WorkshopRecipe.KILN.equals(station) && workshop.isRunning()) {
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5,
                        0.0, 0.025, 0.0);
            }
            if (random.nextBoolean()) {
                level.addParticle(ParticleTypes.SMALL_FLAME,
                        pos.getX() + 0.5 + facing.getStepX() * 0.36,
                        pos.getY() + 0.32,
                        pos.getZ() + 0.5 + facing.getStepZ() * 0.36,
                        0.0, 0.004, 0.0);
            }
        } else if (WorkshopRecipe.CRUCIBLE_FURNACE.equals(station)
                && (workshop.isRunning() || workshop.getStokeTicks() > 0)) {
            int air = workshop.getStokeTicks();
            int flameChance = air >= 320 ? 1 : 2;
            if (random.nextInt(flameChance) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME,
                        pos.getX() + 0.5, pos.getY() + 0.70, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 0.01, 0.012,
                        (random.nextDouble() - 0.5) * 0.01);
            }
            int sparkChance = air >= 320 ? 2 : 3;
            if (air > 0 && random.nextInt(sparkChance) == 0) {
                level.addParticle(ParticleTypes.LAVA,
                        pos.getX() + 0.5, pos.getY() + 0.73, pos.getZ() + 0.5,
                        0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WorkshopBlockEntity workshop) || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (station.equals(WorkshopRecipe.STONE_ANVIL) && stack.is(ModTags.HAMMERS)) {
            if (!level.isClientSide && workshop.work(player)) {
                if (!player.hasInfiniteMaterials()) {
                    stack.hurtAndBreak(1, player, slotFor(hand));
                }
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.65F, 1.35F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Pottery keeps the recipe API batch-based, but the bundled GUI-free interaction no longer
        // requires counting repeated insertion clicks. Sneak-place a valid ingredient on the top
        // plate: center / middle / outer ring loads a 1 / 2 / 3 item batch respectively. Normal
        // insertion remains available for arbitrary pack recipes and larger custom batch sizes.
        if (station.equals(WorkshopRecipe.POTTERY_WHEEL)
                && player.isShiftKeyDown()
                && hit.getDirection() == Direction.UP
                && workshop.getInput().isEmpty()
                && workshop.canInsert(stack)) {
            int targetBatch = potteryBatchForHit(pos, hit);
            if (player.hasInfiniteMaterials() || stack.getCount() >= targetBatch) {
                if (!level.isClientSide) {
                    int inserted = 0;
                    while (inserted < targetBatch && workshop.insert(stack, player.hasInfiniteMaterials())) {
                        inserted++;
                    }
                    if (inserted > 0) {
                        level.playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS,
                                0.45F, 0.90F + inserted * 0.08F);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // Normal right-click favors recipe roles. Sneak-right-click provides an explicit,
        // GUI-free escape hatch when coal/charcoal also appears in a custom recipe role.
        if (player.isShiftKeyDown() && workshop.canInsertFuel(stack)) {
            if (!level.isClientSide) {
                workshop.insertFuel(stack, player.hasInfiniteMaterials());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!workshop.canInsert(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            workshop.insert(stack, player.hasInfiniteMaterials());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WorkshopBlockEntity workshop)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (workshop.takeOutput(player)) {
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown()) {
                return workshop.takeStored(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            if (station.equals(WorkshopRecipe.POTTERY_WHEEL)) {
                workshop.work(player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock()
                && level.getBlockEntity(pos) instanceof WorkshopBlockEntity workshop) {
            for (ItemStack stack : workshop.allStacks()) {
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }

    public static Map<Direction, VoxelShape> makeHorizontalShapes(VoxelShape northShape) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, northShape);
        shapes.put(Direction.EAST, rotate(northShape, 1));
        shapes.put(Direction.SOUTH, rotate(northShape, 2));
        shapes.put(Direction.WEST, rotate(northShape, 3));
        return shapes;
    }

    private static int potteryBatchForHit(BlockPos pos, BlockHitResult hit) {
        double dx = hit.getLocation().x - (pos.getX() + 0.5D);
        double dz = hit.getLocation().z - (pos.getZ() + 0.5D);
        double radiusSq = dx * dx + dz * dz;
        if (radiusSq <= POTTERY_INNER_RADIUS_SQ) {
            return 1;
        }
        if (radiusSq <= POTTERY_MIDDLE_RADIUS_SQ) {
            return 2;
        }
        return 3;
    }

    private static VoxelShape rotate(VoxelShape original, int turns) {
        VoxelShape shape = original;
        for (int i = 0; i < turns; i++) {
            VoxelShape[] rotated = {Shapes.empty()};
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(
                            1.0 - maxZ, minY, minX,
                            1.0 - minZ, maxY, maxX)));
            shape = rotated[0].optimize();
        }
        return shape;
    }

    private static EquipmentSlot slotFor(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }
}
