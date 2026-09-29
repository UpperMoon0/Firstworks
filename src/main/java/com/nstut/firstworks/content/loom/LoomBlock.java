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
        VoxelShape frame = SHAPES.get(state.getValue(FACING));
        if (level.getBlockEntity(pos) instanceof LoomBlockEntity loom && (!loom.getInput().isEmpty() || !loom.getOutput().isEmpty())) {
            double x = 0.5 + loom.getShuttleOffset(0);
            VoxelShape grip = Shapes.box(x - 3.5 / 16, 8.1 / 16, 4.45 / 16, x + 3.5 / 16, 9.9 / 16, 6.25 / 16);
            int turns = switch (state.getValue(FACING)) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
            VoxelShape contents = grip;
            if (!loom.getOutput().isEmpty()) contents = Shapes.or(contents,
                    Shapes.box(3.75 / 16, 6.75 / 16, 7.3 / 16, 12.25 / 16, 11.5 / 16, 7.5 / 16));
            return Shapes.or(frame, rotate(contents, turns));
        }
        return frame;
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
            var point = local(state, pos, hitResult.getLocation());
            if (point.x >= 3.75 / 16 && point.x <= 12.25 / 16 && point.y >= 6.75 / 16
                    && point.y <= 11.5 / 16 && point.z >= 7.25 / 16 && point.z <= 7.55 / 16
                    && loom.takeOutput(player)) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) {
                if (loom.takeInput(player)) {
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
                }
                return InteractionResult.SUCCESS;
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

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LOOM.get(), LoomBlockEntity::tick);
    }

    public static net.minecraft.world.phys.Vec3 local(BlockState state, BlockPos pos, net.minecraft.world.phys.Vec3 hit) {
        double x = hit.x - pos.getX() - 0.5, z = hit.z - pos.getZ() - 0.5;
        return switch (state.getValue(FACING)) {
            case EAST -> new net.minecraft.world.phys.Vec3(0.5 + z, hit.y - pos.getY(), 0.5 - x);
            case SOUTH -> new net.minecraft.world.phys.Vec3(0.5 - x, hit.y - pos.getY(), 0.5 - z);
            case WEST -> new net.minecraft.world.phys.Vec3(0.5 - z, hit.y - pos.getY(), 0.5 + x);
            default -> new net.minecraft.world.phys.Vec3(0.5 + x, hit.y - pos.getY(), 0.5 + z);
        };
    }

    public static boolean hitsShuttle(Player player, LoomBlockEntity loom) {
        if (!(player.pick(player.blockInteractionRange(), 1, false) instanceof BlockHitResult hit)
                || !hit.getBlockPos().equals(loom.getBlockPos())) return false;
        var point = local(loom.getBlockState(), loom.getBlockPos(), hit.getLocation());
        return Math.abs(point.x - (0.5 + loom.getShuttleOffset(0))) <= 3.6 / 16
                && point.y >= 8.0 / 16 && point.y <= 10.0 / 16 && point.z <= 6.4 / 16;
    }

    /** Intersect a broad loom plane, retaining the grip when the ray crosses a frame gap. */
    public static @Nullable net.minecraft.world.phys.Vec3 trackAim(Player player, BlockPos pos, BlockState state) {
        var eye = player.getEyePosition();
        var end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        var from = local(state, pos, eye);
        var to = local(state, pos, end);
        double dz = to.z - from.z;
        if (Math.abs(dz) < 0.00001) return null;
        double t = (5.35 / 16 - from.z) / dz;
        if (t < 0 || t > 1) return null;
        var point = from.lerp(to, t);
        if (point.x < -0.3 || point.x > 1.3 || point.y < 0.1 || point.y > 1.1) return null;
        var hit = player.level().clip(new net.minecraft.world.level.ClipContext(eye, eye.lerp(end, t),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS && !hit.getBlockPos().equals(pos)) return null;
        return point;
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
