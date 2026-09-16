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
            Block.box(1.0, 2.0, 1.0, 4.0, 11.0, 15.0),
            Block.box(12.0, 2.0, 1.0, 15.0, 11.0, 15.0),
            Block.box(4.0, 2.0, 12.0, 12.0, 11.0, 15.0),
            Block.box(4.0, 2.0, 1.0, 12.0, 3.0, 12.0),
            Block.box(1.0, 11.0, 1.0, 6.0, 13.0, 15.0),
            Block.box(10.0, 11.0, 1.0, 15.0, 13.0, 15.0),
            Block.box(6.0, 11.0, 1.0, 10.0, 13.0, 6.0),
            Block.box(6.0, 11.0, 10.0, 10.0, 13.0, 15.0),
            Block.box(5.0, 13.0, 5.0, 6.0, 15.0, 11.0),
            Block.box(10.0, 13.0, 5.0, 11.0, 15.0, 11.0),
            Block.box(6.0, 13.0, 5.0, 10.0, 15.0, 6.0),
            Block.box(6.0, 13.0, 10.0, 10.0, 15.0, 11.0),
            Block.box(4.75, 15.0, 4.75, 6.0, 16.0, 11.25),
            Block.box(10.0, 15.0, 4.75, 11.25, 16.0, 11.25),
            Block.box(6.0, 15.0, 4.75, 10.0, 16.0, 6.0),
            Block.box(6.0, 15.0, 10.0, 10.0, 16.0, 11.25),
            Block.box(4.0, 3.0, 3.0, 4.5, 10.9, 12.0),
            Block.box(11.5, 3.0, 3.0, 12.0, 10.9, 12.0),
            Block.box(4.5, 3.0, 11.5, 11.5, 10.9, 12.0),
            Block.box(4.5, 3.0, 3.0, 11.5, 3.3, 11.5),
            Block.box(4.5, 4.4, 2.0, 11.5, 5.0, 10.5),
            Block.box(4.0, 3.0, 1.0, 4.5, 11.0, 3.0),
            Block.box(11.5, 3.0, 1.0, 12.0, 11.0, 3.0),
            Block.box(4.5, 10.4, 1.4, 11.5, 11.0, 3.0)
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
