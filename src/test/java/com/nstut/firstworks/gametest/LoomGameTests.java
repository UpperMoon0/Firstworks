package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.loom.*;
import com.nstut.firstworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LoomGameTests {
    public static void aim(Player player, LoomBlockEntity loom, double x) {
        aimAt(player, loom, x, 9.0 / 16, 5.35 / 16);
    }

    private static void aimAt(Player player, LoomBlockEntity loom, double x, double y, double z) {
        BlockPos pos = loom.getBlockPos();
        Vec3 target = world(pos, loom.getBlockState().getValue(LoomBlock.FACING), x, y, z);
        Vec3 eye = world(pos, loom.getBlockState().getValue(LoomBlock.FACING), 0.5, 0.8, -1.5);
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        Vec3 delta = target.subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setYHeadRot(player.getYRot());
        player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void guidedCrossingPackingAndReload(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, ModBlocks.LOOM.get());
        LoomBlockEntity loom = h.getBlockEntity(pos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack yarn = new ItemStack(Items.STRING, 4);
        h.assertTrue(loom.insert(yarn, false) && yarn.isEmpty() && loom.getInput().getCount() == 4, "One action must load the batch");
        aim(player, loom, 0.3);
        h.assertTrue(LoomBlock.hitsShuttle(player, loom), "Visible shuttle is not targetable");
        h.assertTrue(loom.guide(player, false), "Could not grab shuttle");
        h.assertTrue(loom.getShuttlePosition() == 0 && loom.getProgress() == 0, "Stationary grip earned work");
        for (int t = 1; t <= 4; t++) h.runAtTickTime(t, () -> {
            aim(player, loom, 0.7);
            loom.guide(player, false);
            h.assertTrue(!loom.guide(player, true), "Repeated sample earned a second movement");
        });
        h.runAtTickTime(5, () -> {
            h.assertTrue(loom.getProgress() == 0 && loom.getShuttlePosition() == 0.5F, "Partial crossing credited a row");
            loom.release(player);
            var saved = loom.saveWithoutMetadata(h.getLevel().registryAccess());
            loom.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(loom.getShuttlePosition() == 0.5F, "Reload lost partial crossing");
        });
        h.runAtTickTime(12, () -> {
            h.assertTrue(loom.getShuttlePosition() == 0.5F && loom.getProgress() == 0, "Idle loom advanced");
            aim(player, loom, 0.5);
            loom.guide(player, false);
        });
        for (int t = 13; t <= 16; t++) h.runAtTickTime(t, () -> { aim(player, loom, 0.75); loom.guide(player, false); });
        h.runAtTickTime(17, () -> {
            h.assertTrue(loom.isPacking() && loom.getProgress() == 0 && !loom.isShuttleRight(), "Row credited before packing");
            var saved = loom.saveWithoutMetadata(h.getLevel().registryAccess());
            loom.loadWithComponents(saved, h.getLevel().registryAccess());
        });
        h.runAtTickTime(24, () -> {
            h.assertTrue(loom.getProgress() == 1 && loom.isShuttleRight() && loom.getShed().equals("B"), "Packing did not commit one row and change shed");
            h.assertTrue(!loom.insert(new ItemStack(Items.STRING), false), "Loading erased partial work");
            h.assertTrue(loom.takeInput(player) && loom.getShuttlePosition() == 0 && loom.getProgress() == 0, "Retrieval failed to reset");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void assistedFourPassesAndExclusiveOwner(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, ModBlocks.LOOM.get());
        LoomBlockEntity loom = h.getBlockEntity(pos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        Player other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setUUID(java.util.UUID.randomUUID());
        loom.insert(new ItemStack(Items.STRING, 4), false);
        for (int t = 1; t <= 56; t++) {
            final int tick = t;
            h.runAtTickTime(t, () -> {
            if (tick > 1) {
                aim(other, loom, 0.3 + 0.4 * loom.getShuttlePosition());
                h.assertTrue(!loom.guide(other, true), "Second player stole an active crossing before owner input");
            }
            aim(player, loom, 0.3);
            loom.guide(player, true);
            aim(other, loom, 0.7);
            h.assertTrue(!loom.guide(other, true), "Second player stole an active crossing");
        });
        }
        h.runAtTickTime(58, () -> {
            h.assertTrue(loom.getInput().isEmpty() && loom.getOutput().is(com.nstut.firstworks.registry.ModItems.CLOTH.get()), "Four assisted crossings did not produce cloth");
            h.assertTrue(loom.takeOutput(player), "Finished output not retrievable");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void shuttleTargetRotatesAndClickCannotWeave(GameTestHelper h) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos pos = new BlockPos(3, 2, 3);
            var state = ModBlocks.LOOM.get().defaultBlockState().setValue(LoomBlock.FACING, facing);
            h.setBlock(pos, state);
            LoomBlockEntity loom = h.getBlockEntity(pos);
            loom.insert(new ItemStack(Items.STRING, 4), false);
            Player player = h.makeMockPlayer(GameType.SURVIVAL);
            aim(player, loom, 0.3);
            var hit = (net.minecraft.world.phys.BlockHitResult) player.pick(player.blockInteractionRange(), 1, false);
            h.assertTrue(LoomBlock.hitsShuttle(player, loom), "Rotated visible shuttle not targetable: " + facing
                    + " actual=" + loom.getBlockState().getValue(LoomBlock.FACING) + " type=" + hit.getType()
                    + " block=" + hit.getBlockPos() + " loom=" + loom.getBlockPos()
                    + " local=" + LoomBlock.local(loom.getBlockState(), loom.getBlockPos(), hit.getLocation()));
            state.useWithoutItem(h.getLevel(), player, hit);
            h.assertTrue(loom.getProgress() == 0 && loom.getShuttlePosition() == 0, "Click bypassed crossing");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            h.assertTrue(!loom.guide(player, true), "Nonempty hand operated loom");
            h.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void overlappingRecipeAutomaticallySelectsItsShed(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, ModBlocks.LOOM.get());
        LoomBlockEntity loom = h.getBlockEntity(pos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        loom.insert(new ItemStack(Items.EMERALD, 3), false);
        h.assertTrue(loom.getMatchingRecipe().equals(loom.getActiveRecipe())
                && loom.getActiveRecipe().orElseThrow().value().inputCount() == 3, "Preview and active recipe disagree");
        for (int t = 1; t <= 8; t++) h.runAtTickTime(t, () -> {
            aim(player, loom, 0.3);
            loom.guide(player, true);
            h.assertTrue(loom.getShed().equals("B"), "Recipe's B shed was not selected automatically");
        });
        h.runAtTickTime(16, () -> {
            h.assertTrue(loom.getInput().isEmpty() && loom.getOutput().is(Items.BOOK), "Overlapping recipe consumed/produced the wrong batch");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void uncollectedOutputLeavesNextFabricVisibleAndCollectsSeparately(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, ModBlocks.LOOM.get());
        LoomBlockEntity loom = h.getBlockEntity(pos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        loom.insert(new ItemStack(Items.STRING, 8), false);
        for (int t = 1; t <= 70; t++) h.runAtTickTime(t, () -> {
            aim(player, loom, 0.3);
            loom.guide(player, true);
        });
        h.runAtTickTime(72, () -> {
            h.assertTrue(loom.getOutput().getCount() == 1 && loom.getInput().getCount() == 4
                    && loom.getProgress() == 1 && loom.getFabricFraction() == 0.25F,
                    "Uncollected output hid the next batch's one-row fabric");
            h.assertTrue(loom.interactionHint().getString().contains("shuttle"), "Ready output hid active weaving hint");
            loom.release(player);
            // Output is collected from the frame, without a separate output model or hitbox.
            net.minecraft.world.phys.BlockHitResult hit = null;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                h.setBlock(pos, loom.getBlockState().setValue(LoomBlock.FACING, facing));
                aimAt(player, loom, 0.5, 14.5 / 16, 5.25 / 16);
                hit = (net.minecraft.world.phys.BlockHitResult) player.pick(player.blockInteractionRange(), 1, false);
                h.assertTrue(hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                        && hit.getBlockPos().equals(loom.getBlockPos()),
                        "Loom frame is not targetable for collection: " + facing);
            }
            loom.getBlockState().useWithoutItem(h.getLevel(), player, hit);
            h.assertTrue(loom.getOutput().isEmpty() && loom.getFabricFraction() == 0.25F
                    && loom.getInput().getCount() == 4,
                    "Collecting finished cloth changed the next batch");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void delayedHeldSampleKeepsGripAcrossFrameGapAndReleaseAllowsTakeover(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        h.setBlock(pos, ModBlocks.LOOM.get());
        LoomBlockEntity loom = h.getBlockEntity(pos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        Player other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setUUID(java.util.UUID.randomUUID());
        loom.insert(new ItemStack(Items.STRING, 4), false);
        aim(player, loom, 0.3);
        h.assertTrue(loom.guide(player, true), "Initial grip failed");
        h.runAtTickTime(12, () -> {
            aimAt(player, loom, 0.7, 10.7 / 16, 5.35 / 16);
            h.assertTrue(!LoomBlock.hitsShuttle(player, loom), "Delayed sample test did not aim through a gap");
            h.assertTrue(loom.guide(player, true) && loom.getShuttlePosition() == 0.25F,
                    "Delayed held sample required a new shuttle target");
        });
        h.runAtTickTime(13, () -> {
            aim(other, loom, 0.4);
            h.assertTrue(!loom.guide(other, false), "Competing operator stole refreshed grip");
            loom.release(player);
        });
        h.runAtTickTime(14, () -> {
            aim(other, loom, 0.4);
            h.assertTrue(loom.guide(other, false), "Release did not allow a new operator");
        });
        h.runAtTickTime(26, () -> {
            aim(player, loom, 0.4);
            h.assertTrue(loom.guide(player, false), "A stale disconnected operator locked the shuttle");
            h.succeed();
        });
    }

    private static Vec3 world(BlockPos pos, Direction facing, double x, double y, double z) {
        return switch (facing) {
            case EAST -> new Vec3(pos.getX() + 1 - z, pos.getY() + y, pos.getZ() + x);
            case SOUTH -> new Vec3(pos.getX() + 1 - x, pos.getY() + y, pos.getZ() + 1 - z);
            case WEST -> new Vec3(pos.getX() + z, pos.getY() + y, pos.getZ() + 1 - x);
            default -> new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
        };
    }
}
