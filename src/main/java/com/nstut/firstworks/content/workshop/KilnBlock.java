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

public final class KilnBlock extends WorkshopBlock {
    public static final MapCodec<KilnBlock> CODEC = simpleCodec(KilnBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = makeHorizontalShapes(Shapes.or(
            Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),
            Block.box(1.0, 2.0, 1.0, 4.0, 12.0, 15.0),
            Block.box(12.0, 2.0, 1.0, 15.0, 12.0, 15.0),
            Block.box(4.0, 2.0, 11.0, 12.0, 12.0, 15.0),
            Block.box(4.0, 2.0, 1.0, 12.0, 4.2, 4.0),
            Block.box(3.0, 10.0, 1.0, 13.0, 13.0, 15.0),
            Block.box(5.0, 12.2, 5.0, 11.0, 15.2, 11.0),
            Block.box(6.0, 14.8, 6.0, 10.0, 16.0, 10.0)
    ).optimize());

    public KilnBlock(Properties properties) {
        super(properties, WorkshopRecipe.KILN);
    }

    @Override
    protected MapCodec<? extends KilnBlock> codec() {
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
