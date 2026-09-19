package com.nstut.firstworks.content.loom;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import java.util.EnumMap;
import java.util.Map;

public class LoomBlock extends BaseEntityBlock {
    public enum Control { LEFT, RIGHT, SHED, NONE }

    /** Regions use the north-facing model's coordinates, independent of the player's view. */
    public static Control controlAt(BlockState state, BlockPos pos, net.minecraft.world.phys.Vec3 hit) {
        double x = hit.x - pos.getX() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;
        double localX = 0.5 + switch (state.getValue(FACING)) {
            case EAST -> z;
            case SOUTH -> -x;
            case WEST -> -z;
            default -> x;
        };
        double y = hit.y - pos.getY();
        if (y >= 2.75 / 16.0 && y <= 4.25 / 16.0 && localX >= 2.5 / 16.0 && localX <= 13.5 / 16.0) return Control.SHED;
        if (y >= 5.25 / 16.0 && y <= 13.0 / 16.0) return localX < 0.5 ? Control.LEFT : Control.RIGHT;
        return Control.NONE;
    }
    public static final MapCodec<LoomBlock> CODEC = simpleCodec(LoomBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(0.75, 0, 2, 4.25, 1.75, 14), box(11.75, 0, 2, 15.25, 1.75, 14),
            box(1.25, 1, 6.25, 3.75, 15, 9.75), box(12.25, 1, 6.25, 14.75, 15, 9.75),
            box(0.5, 13.75, 5.25, 15.5, 16, 10.75), box(2.5, 2.75, 5.5, 13.5, 4.25, 10.5),
            box(3, 11.5, 5.75, 13, 13, 10.25), box(3, 5.25, 5.75, 13, 6.75, 10.25)
    ).optimize();
    private static final Map<Direction, VoxelShape> SHAPES = makeShapes();

    public LoomBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(net.minecraft.network.chat.Component.translatable("hint.firstworks.loom.controls"));
        tooltip.add(net.minecraft.network.chat.Component.translatable("hint.firstworks.loom.retrieve"));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LoomBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof LoomBlockEntity loom) || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!loom.canInsert(stack)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && loom.insert(stack, player.getAbilities().instabuild)) {
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.65F, 1.15F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof LoomBlockEntity loom)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (loom.takeOutput(player)) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) {
                if (loom.takeInput(player)) {
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            Control control = controlAt(state, pos, hitResult.getLocation());
            if (control == Control.SHED) loom.changeShed();
            else if (control == Control.LEFT || control == Control.RIGHT) {
                if (!loom.weave(player, control == Control.RIGHT)) player.displayClientMessage(loom.interactionHint(control), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof LoomBlockEntity loom) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, loom.getInput());
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, loom.getOutput());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    private static Map<Direction, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, NORTH_SHAPE);
        shapes.put(Direction.EAST, rotate(NORTH_SHAPE, 1));
        shapes.put(Direction.SOUTH, rotate(NORTH_SHAPE, 2));
        shapes.put(Direction.WEST, rotate(NORTH_SHAPE, 3));
        return shapes;
    }

    private static VoxelShape rotate(VoxelShape original, int turns) {
        VoxelShape shape = original;
        for (int i = 0; i < turns; i++) {
            VoxelShape[] rotated = { Shapes.empty() };
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(1.0 - maxZ, minY, minX,
                            1.0 - minZ, maxY, maxX)));
            shape = rotated[0].optimize();
        }
        return shape;
    }
}
