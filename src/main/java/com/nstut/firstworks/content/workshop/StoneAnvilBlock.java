package com.nstut.firstworks.content.workshop;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public final class StoneAnvilBlock extends WorkshopBlock {
    public static final MapCodec<StoneAnvilBlock> CODEC = simpleCodec(StoneAnvilBlock::new);
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2.5, 14),
            Block.box(3, 2.5, 3, 13, 4.2, 13),
            Block.box(5, 4.2, 5, 11, 7.2, 11),
            Block.box(3, 7.2, 3, 13, 9.4, 13),
            Block.box(2, 9.4, 3, 14, 10.6, 13),
            Block.box(5, 8, 1.5, 11, 9.7, 4),
            Block.box(6, 8.3, 0.5, 10, 9.7, 1.5),
            Block.box(6, 3.4, 1.7, 10, 5.2, 4.7)
    ).optimize();
    private static final Map<Direction, VoxelShape> SHAPES = makeHorizontalShapes(NORTH_SHAPE);

    public StoneAnvilBlock(Properties properties) {
        super(properties, WorkshopRecipe.STONE_ANVIL);
    }

    public static Vec3 localHit(BlockState state, BlockPos pos, Vec3 hit) {
        double x = hit.x - pos.getX() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;
        return switch (state.getValue(FACING)) {
            case EAST -> new Vec3(z + 0.5, hit.y - pos.getY(), 0.5 - x);
            case SOUTH -> new Vec3(0.5 - x, hit.y - pos.getY(), 0.5 - z);
            case WEST -> new Vec3(0.5 - z, hit.y - pos.getY(), x + 0.5);
            default -> new Vec3(x + 0.5, hit.y - pos.getY(), z + 0.5);
        };
    }

    public static String actionAt(BlockState state, BlockPos pos, BlockHitResult hit) {
        var local = localHit(state, pos, hit.getLocation());
        if (hit.getDirection() != Direction.UP || local.y < 9.0 / 16.0) return "none";
        if (local.z < 3.0 / 16.0) return "bend";
        if (local.x < 4.0 / 16.0 || local.x > 12.0 / 16.0 || local.z > 11.0 / 16.0) return "draw";
        return "flatten";
    }

    public static VoxelShape zone(String action) {
        return switch (action) {
            case "bend" -> Shapes.or(Block.box(6, 9.71, 0.5, 10, 9.73, 1.5), Block.box(5, 9.71, 1.5, 11, 9.73, 3));
            case "draw" -> Shapes.or(Block.box(2, 10.61, 3, 4, 10.63, 13),
                    Block.box(12, 10.61, 3, 14, 10.63, 13), Block.box(4, 10.61, 11, 12, 10.63, 13));
            case "flatten" -> Block.box(4, 10.61, 3, 12, 10.63, 11);
            default -> Shapes.empty();
        };
    }

    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("hint.firstworks.anvil.controls"));
        tooltip.add(Component.translatable("hint.firstworks.anvil.reheat"));
    }

    @Override
    protected MapCodec<? extends StoneAnvilBlock> codec() {
        return CODEC;
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
}
