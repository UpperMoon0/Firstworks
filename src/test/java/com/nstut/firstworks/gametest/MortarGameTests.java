package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.MortarGrindingRecipe;
import com.nstut.firstworks.content.mortar.MortarBlockEntity;
import com.nstut.firstworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MortarGameTests {
    private static final BlockPos POS = new BlockPos(4, 1, 4);
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void overlappingRecipesUseTheDisplayedStage(GameTestHelper h) {
        var mortar = mortar(h);
        var p = operator(h, false);
        mortar.insert(new ItemStack(Items.EMERALD), false);
        check(h, mortar.getStage().orElseThrow().action().equals("crush"), "Small batch displays wrong action");
        check(h, mortar.operate(p, "crush") && mortar.getOutput().is(Items.PAPER), "Small crush batch failed");
        mortar.takeOutput(p);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.runAtTickTime(5, () -> {
            check(h, mortar.getItemHandler(null).insertItem(0, new ItemStack(Items.EMERALD, 3), false).isEmpty(), "Overlapping small recipe prevented larger batch loading");
            check(h, mortar.getActiveRecipe().orElseThrow().value().inputCount() == 3, "Largest available batch not selected");
            check(h, mortar.getStage().orElseThrow().action().equals("grind"), "Large batch displays wrong action");
            check(h, !mortar.operate(p, "crush"), "Large batch accepted small recipe action");
            aim(h, p, true);
            check(h, mortar.operate(p, "grind"), "Large grind batch rejected");
            check(h, mortar.getInput().isEmpty() && mortar.getOutput().is(Items.BOOK), "Large batch output/consumption wrong");
            h.succeed();
        });
    }

    private static Player operator(GameTestHelper h, boolean grind) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        aim(h, p, grind);
        return p;
    }
    private static void aim(GameTestHelper h, Player p, boolean grind) {
        BlockPos pos = h.absolutePos(POS);
        p.moveTo(pos.getX() + (grind ? 0.74 : 0.5), pos.getY() + 0.65, pos.getZ() + 0.5, 0, 90);
        p.setXRot(90);
    }
    private static MortarBlockEntity mortar(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.MORTAR_AND_PESTLE.get());
        return h.getBlockEntity(POS);
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void crushThenGrindReleaseReloadAndLifecycle(GameTestHelper h) {
        MortarBlockEntity mortar = mortar(h);
        Player p = operator(h, true);
        mortar.insert(new ItemStack(Items.AMETHYST_SHARD), false);
        check(h, !mortar.operate(p, "grind"), "Wrong first stage advanced");
        aim(h, p, false);
        check(h, mortar.operate(p, "crush") && mortar.getStageIndex() == 1, "Crush did not advance stage");
        var recipe = mortar.getActiveRecipe().orElseThrow().value();
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var codec = new MortarGrindingRecipe.Serializer().streamCodec();
            codec.encode(buffer, recipe);
            check(h, codec.decode(buffer).processing().equals(recipe.processing()), "Stage network codec lost data");
        } finally { buffer.release(); }
        h.runAtTickTime(2, () -> {
            aim(h, p, true);
            check(h, mortar.operate(p, "grind"), "Grind rejected after crush");
            Player second = operator(h, true);
            check(h, !mortar.operate(second, "grind"), "Multiple players double-advanced one tick");
            mortar.stopGrinding(p);
            check(h, !mortar.isGrinding(), "Release did not stop active grinding");
        });
        h.runAtTickTime(8, () -> {
            check(h, mortar.getStageIndex() == 1 && mortar.getStageProgress() == 1 && mortar.getOutput().isEmpty(), "Paused mortar advanced autonomously");
            var saved = mortar.saveWithoutMetadata(h.getLevel().registryAccess());
            mortar.loadWithComponents(saved, h.getLevel().registryAccess());
            check(h, !mortar.isGrinding() && mortar.getStageIndex() == 1 && mortar.getStageProgress() == 1, "Reload lost partial stage or restored unowned activity");
            check(h, mortar.operate(p, "grind"), "Resume rejected");
        });
        h.runAtTickTime(10, () -> {
            check(h, mortar.operate(p, "grind"), "Final grind rejected");
            check(h, mortar.getInput().isEmpty() && mortar.getOutput().is(Items.DIAMOND), "Wrong completed batch");
            check(h, mortar.getPersistentData().getInt("starts") == 1 && mortar.getPersistentData().getInt("completions") == 1, "Lifecycle hooks did not fire exactly once");
            check(h, mortar.takeOutput(p), "Output retrieval failed");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void crushOnlyAndInterruption(GameTestHelper h) {
        var mortar = mortar(h);
        var p = operator(h, false);
        mortar.insert(new ItemStack(Items.PAPER), false);
        check(h, mortar.operate(p, "crush"), "First crush rejected");
        check(h, !mortar.operate(p, "crush"), "Duplicate crush advanced");
        h.runAtTickTime(5, () -> {
            check(h, mortar.operate(p, "crush"), "Second crush rejected");
            check(h, mortar.getOutput().is(Items.BOOK), "Crush-only recipe failed");
            mortar.takeOutput(p);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            mortar.insert(new ItemStack(Items.PAPER), false);
        });
        h.runAtTickTime(10, () -> {
            check(h, mortar.operate(p, "crush"), "Second batch crush rejected");
            check(h, mortar.takeInput(p) && mortar.getStageProgress() == 0, "Retrieval did not reset stage state");
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            mortar.insert(new ItemStack(Items.SUGAR), false);
            check(h, mortar.getActiveRecipe().orElseThrow().value().stages().getFirst().action().equals("crush"), "KubeJS custom recipe lost stage metadata");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void legacyGrindRequiresValidHeldInput(GameTestHelper h) {
        var mortar = mortar(h);
        var p = operator(h, true);
        check(h, !mortar.insert(new ItemStack(Items.COBBLESTONE), false), "Invalid ingredient accepted");
        mortar.insert(new ItemStack(Items.FEATHER), false);
        check(h, !mortar.operate(p, "grind"), "Insufficient input advanced");
        mortar.insert(new ItemStack(Items.FEATHER), false);
        aim(h, p, false);
        check(h, !mortar.operate(p, "grind"), "Center hit was accepted as grinding");
        aim(h, p, true);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        check(h, !mortar.operate(p, "grind"), "Held item did not block empty-hand controls");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        check(h, mortar.operate(p, "grind"), "Fallback grind rejected");
        h.runAtTickTime(4, () -> {
            check(h, !mortar.isGrinding() && mortar.getStageProgress() == 1, "Missing held-input updates did not pause");
            p.setPos(p.getX() + 12, p.getY(), p.getZ());
            check(h, !mortar.operate(p, "grind"), "Distant player advanced");
            aim(h, p, true);
            check(h, mortar.operate(p, "grind"), "Fallback resume rejected");
        });
        h.runAtTickTime(6, () -> {
            check(h, mortar.operate(p, "grind") && mortar.getOutput().is(Items.STRING), "Fallback did not complete duration-based grinding");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void scriptCancellationDoesNotSpamOrConsume(GameTestHelper h) {
        var mortar = mortar(h);
        var p = operator(h, true);
        mortar.insert(new ItemStack(Items.PRISMARINE_SHARD), false);
        check(h, !mortar.operate(p, "grind"), "Script veto ignored");
        h.runAtTickTime(3, () -> {
            check(h, !mortar.operate(p, "grind") && mortar.getInput().getCount() == 1, "Cancelled work consumed input");
            check(h, mortar.getPersistentData().getInt("starts") == 1, "Held input spammed cancelled lifecycle hook");
            h.succeed();
        });
    }
    private static void check(GameTestHelper h, boolean value, String message) { h.assertTrue(value, message); }
}
