package com.nstut.firstworks.content.workshop;

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
import java.util.List;

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
    private record WorkingSurface(double x0, double z0, double x1, double z1, double height) {
        boolean contains(Vec3 hit) {
            return hit.x >= x0 / 16 && hit.x < x1 / 16
                    && hit.z >= z0 / 16 && hit.z < z1 / 16
                    && Math.abs(hit.y - height / 16) < 0.0001;
        }
        VoxelShape outline() {
            return Block.box(x0, height + 0.01, z0, x1, height + 0.03, z1);
        }
    }
    // All coordinates describe exposed model surfaces in the NORTH orientation.
    // Half-open rectangles assign a shared boundary to exactly one action.
    private static final Map<String, List<WorkingSurface>> WORKING_SURFACES = Map.of(
            "flatten", List.of(new WorkingSurface(6, 6, 10, 10, 10.6)),
            "draw", List.of(new WorkingSurface(2, 3, 6, 13, 10.6),
                    new WorkingSurface(10, 3, 14, 13, 10.6),
                    new WorkingSurface(6, 3, 10, 6, 10.6),
                    new WorkingSurface(6, 10, 10, 13, 10.6)),
            "bend", List.of(new WorkingSurface(6, 0.5, 10, 1.5, 9.7),
                    new WorkingSurface(5, 1.5, 11, 3, 9.7)));
    private static final Map<String, Map<Direction, VoxelShape>> ZONES = Map.of(
            "flatten", makeHorizontalShapes(zone("flatten")),
            "draw", makeHorizontalShapes(zone("draw")),
            "bend", makeHorizontalShapes(zone("bend")));

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
        if (hit.getDirection() != Direction.UP) return "none";
        for (var entry : WORKING_SURFACES.entrySet()) {
            if (entry.getValue().stream().anyMatch(surface -> surface.contains(local))) return entry.getKey();
        }
        return "none";
    }

    public static VoxelShape zone(String action) {
        return WORKING_SURFACES.getOrDefault(action, List.of()).stream()
                .map(WorkingSurface::outline).reduce(Shapes.empty(), Shapes::or).optimize();
    }

    public static VoxelShape zone(BlockState state, String action) {
        var rotated = ZONES.get(action);
        return rotated == null ? Shapes.empty() : rotated.get(state.getValue(FACING));
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
