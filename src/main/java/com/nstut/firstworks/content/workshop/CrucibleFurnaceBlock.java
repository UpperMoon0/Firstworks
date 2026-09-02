package com.nstut.firstworks.content.workshop;

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

public final class CrucibleFurnaceBlock extends WorkshopBlock {
    public static final MapCodec<CrucibleFurnaceBlock> CODEC = simpleCodec(CrucibleFurnaceBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = makeHorizontalShapes(Shapes.or(
            Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),
            Block.box(1.0, 2.0, 1.0, 4.2, 11.5, 15.0),
            Block.box(11.8, 2.0, 1.0, 15.0, 11.5, 15.0),
            Block.box(4.2, 2.0, 11.5, 11.8, 11.5, 15.0),
            Block.box(4.2, 2.0, 1.0, 11.8, 5.2, 4.2),
            Block.box(3.2, 9.5, 3.2, 12.8, 12.5, 12.8),
            Block.box(6.5, 3.0, 0.0, 9.5, 5.2, 4.8)
    ).optimize());

    public CrucibleFurnaceBlock(Properties properties) {
        super(properties, WorkshopRecipe.CRUCIBLE_FURNACE);
    }

    @Override
    protected MapCodec<? extends CrucibleFurnaceBlock> codec() {
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
