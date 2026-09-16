package com.nstut.firstworks.content.workshop;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PotteryWheelBlock extends WorkshopBlock {
    public static final MapCodec<PotteryWheelBlock> CODEC = simpleCodec(PotteryWheelBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.0, 0.0, 2.0, 5.0, 7.0, 5.0),
            Block.box(11.0, 0.0, 2.0, 14.0, 7.0, 5.0),
            Block.box(2.0, 0.0, 11.0, 5.0, 7.0, 14.0),
            Block.box(11.0, 0.0, 11.0, 14.0, 7.0, 14.0),
            Block.box(2.0, 5.5, 3.0, 14.0, 7.0, 5.0),
            Block.box(2.0, 5.5, 11.0, 14.0, 7.0, 13.0),
            Block.box(7.2, 1.5, 7.2, 8.8, 9.2, 8.8),
            Block.box(3.7, 2.3, 3.7, 12.3, 3.1, 12.3),
            Block.box(2.8, 8.1, 2.8, 13.2, 9.15, 13.2)
    ).optimize();

    public PotteryWheelBlock(Properties properties) {
        super(properties, WorkshopRecipe.POTTERY_WHEEL);
    }

    @Override
    protected MapCodec<? extends PotteryWheelBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return SHAPE;
    }
}
