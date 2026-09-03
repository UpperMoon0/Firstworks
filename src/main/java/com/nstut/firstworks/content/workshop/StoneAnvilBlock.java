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

public final class StoneAnvilBlock extends WorkshopBlock {
    public static final MapCodec<StoneAnvilBlock> CODEC = simpleCodec(StoneAnvilBlock::new);
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(2.0, 0.0, 2.0, 14.0, 4.2, 14.0),
            Block.box(5.0, 4.2, 5.0, 11.0, 7.2, 11.0),
            Block.box(2.0, 7.2, 3.0, 14.0, 10.6, 13.0),
            Block.box(5.0, 8.0, 0.5, 11.0, 9.7, 4.0),
            Block.box(6.0, 3.4, 1.7, 10.0, 5.2, 5.0)
    ).optimize();
    private static final Map<Direction, VoxelShape> SHAPES = makeHorizontalShapes(NORTH_SHAPE);

    public StoneAnvilBlock(Properties properties) {
        super(properties, WorkshopRecipe.STONE_ANVIL);
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
