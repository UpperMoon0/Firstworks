package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.loom.LoomBlock;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import com.nstut.firstworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LoomGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void overlappingRecipesUseTheDisplayedPattern(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, ModBlocks.LOOM.get());
        LoomBlockEntity loom = h.getBlockEntity(pos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        loom.insert(new ItemStack(Items.EMERALD), false);
        check(h, loom.getMatchingRecipe().equals(loom.getActiveRecipe()), "Display selected a different recipe");
        check(h, loom.getRequiredStrokes() == 1 && loom.weave(player, false), "Small A-pattern batch failed");
        check(h, loom.getOutput().is(Items.PAPER), "Small batch produced wrong output");
        loom.takeOutput(player);
        h.runAtTickTime(3, () -> {
            loom.getItemHandler(null).insertItem(0, new ItemStack(Items.EMERALD, 3), false);
            check(h, loom.getMatchingRecipe().equals(loom.getActiveRecipe()), "Large batch display disagrees");
            check(h, loom.getMatchingRecipe().orElseThrow().value().inputCount() == 3, "Largest available batch not selected");
            check(h, !loom.weave(player, loom.isShuttleRight()), "Large batch ignored B pattern");
            loom.changeShed();
            check(h, loom.weave(player, loom.isShuttleRight()), "Large B-pattern batch failed");
            check(h, loom.getInput().isEmpty() && loom.getOutput().is(Items.BOOK), "Large batch output/consumption wrong");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void patternWrongSideShedAndReload(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.LOOM.get());
        LoomBlockEntity loom = helper.getBlockEntity(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        check(helper, loom.insert(new ItemStack(Items.PAPER), false), "Pattern input rejected");
        check(helper, loom.getRequiredStrokes() == 4, "Pattern pass override ignored");
        var recipe = loom.getActiveRecipe().orElseThrow().value();
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var codec = new com.nstut.firstworks.content.loom.LoomRecipe.Serializer().streamCodec();
            codec.encode(buffer, recipe);
            var decoded = codec.decode(buffer);
            check(helper, decoded.weaving().equals(recipe.weaving()) && decoded.strokes() == recipe.strokes(), "Recipe synchronization lost pattern or pass override");
        } finally {
            buffer.release();
        }
        check(helper, !loom.weave(player, true), "Wrong side advanced");
        check(helper, loom.weave(player, false), "Initial A pass rejected");
        check(helper, !loom.weave(player, true), "Two players could advance in the same tick");
        check(helper, !loom.insert(new ItemStack(Items.PAPER), false), "Insertion erased partial progress");
        var saved = loom.saveWithoutMetadata(helper.getLevel().registryAccess());
        loom.changeShed();
        loom.loadWithComponents(saved, helper.getLevel().registryAccess());
        check(helper, loom.getProgress() == 1 && loom.isShuttleRight() && loom.getShed().equals("A"), "Reload lost weave state");
        helper.runAtTickTime(2, () -> {
            check(helper, !loom.weave(player, false), "Repeated side advanced");
            check(helper, loom.weave(player, true), "Second A pass rejected");
        });
        helper.runAtTickTime(4, () -> {
            check(helper, !loom.weave(player, false), "Wrong shed advanced");
            loom.changeShed();
            var state = loom.saveWithoutMetadata(helper.getLevel().registryAccess());
            loom.loadWithComponents(state, helper.getLevel().registryAccess());
            check(helper, loom.getShed().equals("B"), "B shed did not survive reload");
            check(helper, loom.weave(player, false), "B pass rejected");
        });
        helper.runAtTickTime(6, () -> {
            check(helper, loom.weave(player, true), "Final B pass rejected");
            check(helper, loom.getInput().isEmpty() && loom.getOutput().is(Items.BOOK), "Wrong completion output/input");
            check(helper, loom.takeOutput(player) && loom.getOutput().isEmpty(), "Output could not be retrieved");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void fallbackRequiresInputAndManualWork(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModBlocks.LOOM.get());
        LoomBlockEntity loom = helper.getBlockEntity(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        check(helper, !loom.insert(new ItemStack(Items.COBBLESTONE), false), "Unsupported input accepted");
        check(helper, !loom.weave(player, false), "Empty loom advanced");
        loom.insert(new ItemStack(Items.STRING), false);
        check(helper, !loom.weave(player, false), "Insufficient input advanced");
        loom.getItemHandler(null).insertItem(0, new ItemStack(Items.STRING, 3), false);
        check(helper, loom.getActiveRecipe().orElseThrow().value().weaving().equals(com.nstut.firstworks.content.loom.LoomRecipe.Weaving.DEFAULT), "Legacy recipe lost fallback");
        check(helper, loom.weave(player, false), "Fallback A pass rejected");
        helper.runAtTickTime(10, () -> {
            check(helper, loom.getProgress() == 1 && loom.getOutput().isEmpty(), "Loom advanced without manual operation");
            check(helper, !loom.weave(player, true), "Fallback did not require B shed");
            loom.changeShed();
            check(helper, loom.weave(player, true), "Fallback did not resume");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void controlsRotateWithModelAndRetrievalResetsState(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos pos = new BlockPos(3 + facing.get2DDataValue() * 2, 1, 6);
            var state = ModBlocks.LOOM.get().defaultBlockState().setValue(LoomBlock.FACING, facing);
            helper.setBlock(pos, state);
            LoomBlockEntity loom = helper.getBlockEntity(pos);
            loom.insert(new ItemStack(Items.PAPER), false);
            BlockPos absolute = helper.absolutePos(pos);
            for (boolean right : new boolean[]{false, true}) {
                Vec3 location = localHit(absolute, facing, right ? 0.85 : 0.15, 0.6, 0.4);
                check(helper, LoomBlock.controlAt(state, absolute, location) == (right ? LoomBlock.Control.RIGHT : LoomBlock.Control.LEFT), "Rotated shuttle region mismatch");
            }
            var shedHit = new BlockHitResult(localHit(absolute, facing, 0.5, 3.5 / 16.0, 0.4), facing, absolute, false);
            state.useWithoutItem(helper.getLevel(), player, shedHit);
            check(helper, loom.getShed().equals("B") && loom.getProgress() == 0, "Treadle did not change shed independently");
            loom.changeShed();
            var leftHit = new BlockHitResult(localHit(absolute, facing, 0.15, 0.6, 0.4), facing, absolute, false);
            state.useWithoutItem(helper.getLevel(), player, leftHit);
            check(helper, loom.getProgress() == 1, "Block interaction did not throw shuttle");
            check(helper, loom.takeInput(player), "Interrupted input not retrievable");
            check(helper, loom.getProgress() == 0 && !loom.isShuttleRight() && loom.getShed().equals("A"), "Retrieval left stale state");
        }
        helper.succeed();
    }

    private static Vec3 localHit(BlockPos pos, Direction facing, double x, double y, double z) {
        return switch (facing) {
            case EAST -> new Vec3(pos.getX() + 1 - z, pos.getY() + y, pos.getZ() + x);
            case SOUTH -> new Vec3(pos.getX() + 1 - x, pos.getY() + y, pos.getZ() + 1 - z);
            case WEST -> new Vec3(pos.getX() + z, pos.getY() + y, pos.getZ() + 1 - x);
            default -> new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
        };
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, message);
    }
}
