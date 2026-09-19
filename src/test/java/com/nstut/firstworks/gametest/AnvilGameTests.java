package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.workshop.*;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AnvilGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void coolingOrderReloadAndCustomVisualFallback(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity anvil = h.getBlockEntity(pos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STONE_HAMMER.get()));
        anvil.insert(new ItemStack(Items.AMETHYST_SHARD), false);
        check(h, !anvil.forge(player, "draw"), "Cold workpiece advanced");
        check(h, !anvil.reheat(player), "Reheated without heat source");
        h.setBlock(pos.south(), Blocks.CAMPFIRE);
        check(h, anvil.reheat(player), "Lit campfire did not heat");
        check(h, !anvil.work(player), "Generic work bypassed forge sequence");
        check(h, !anvil.forge(player, "bend") && anvil.getProgress() == 0, "Wrong order advanced");
        check(h, anvil.forge(player, "draw"), "Correct action rejected");
        check(h, !anvil.forge(player, "bend"), "Two simultaneous strikes advanced");
        check(h, !anvil.insert(new ItemStack(Items.AMETHYST_SHARD), false), "Insertion reset partial forging");
        var saved = anvil.saveWithoutMetadata(h.getLevel().registryAccess());
        anvil.loadWithComponents(saved, h.getLevel().registryAccess());
        check(h, anvil.getProgress() == 1 && anvil.getForgeHeat() == 3 && anvil.getLastForgeAction().equals("draw"), "Reload lost heat/action/shape state");
        h.runAtTickTime(5, () -> {
            check(h, anvil.getForgeHeat() == 0 && !anvil.forge(player, "bend"), "Cooling did not block forging");
            check(h, anvil.getProgress() == 1, "Cooling reset partial work");
            check(h, anvil.reheat(player), "Reheat failed after cooling");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            check(h, !anvil.forge(player, "bend"), "Missing hammer advanced");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.STONE_HAMMER.get()));
            BlockPos absolute = h.absolutePos(pos);
            var hit = new BlockHitResult(new Vec3(absolute.getX() + 0.5, absolute.getY() + 9.7 / 16.0, absolute.getZ() + 0.1), Direction.UP, absolute, false);
            check(h, anvil.getBlockState().useWithoutItem(h.getLevel(), player, hit) == net.minecraft.world.InteractionResult.PASS,
                    "Empty main hand swallowed offhand hammer interaction");
            anvil.getBlockState().useItemOn(player.getOffhandItem(), h.getLevel(), player, InteractionHand.OFF_HAND, hit);
            check(h, player.getOffhandItem().getDamageValue() == 1, "Offhand hammer strike did not consume one durability");
            check(h, anvil.getOutput().is(Items.DIAMOND) && anvil.getInput().isEmpty() && anvil.getForgeHeat() == 0, "Wrong completion state");
            check(h, anvil.takeOutput(player), "Output not retrievable");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void arbitrarySmashingAndRecipeNetworkRoundTrip(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity anvil = h.getBlockEntity(pos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STONE_HAMMER.get()));
        anvil.insert(new ItemStack(Items.FLINT), false);
        check(h, anvil.forge(player, "flatten") && anvil.forge(player, "flatten"), "Legacy smashing recipe stopped working");
        check(h, anvil.getOutput().is(Items.GRAVEL), "Legacy smashing produced wrong output");
        anvil.takeOutput(player);
        anvil.insert(new ItemStack(ModItems.ANNEALED_COPPER_BILLET.get()), false);
        var recipe = anvil.activeRecipe().orElseThrow().value();
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var codec = new WorkshopRecipe.Serializer().streamCodec();
            codec.encode(buffer, recipe);
            var decoded = codec.decode(buffer);
            check(h, decoded.forge().equals(recipe.forge()), "Network lost forge sequence/profile/heat");
        } finally { buffer.release(); }
        check(h, anvil.takeStored(player) && anvil.getProgress() == 0 && anvil.getForgeHeat() == 0, "Retrieval left stale work");
        anvil.insert(new ItemStack(Items.BLAZE_POWDER), false);
        check(h, anvil.activeRecipe().orElseThrow().value().forge().orElseThrow().actions().size() == 2, "KubeJS custom recipe lost forge metadata");
        check(h, anvil.forge(player, "draw"), "Cold-capable KubeJS forge recipe rejected");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void legacyPartialProgressMigratesToForgeSequence(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity anvil = h.getBlockEntity(pos);
        anvil.insert(new ItemStack(ModItems.ANNEALED_COPPER_BILLET.get()), false);

        var legacy = anvil.saveWithoutMetadata(h.getLevel().registryAccess());
        legacy.putInt("Progress", 6);
        legacy.putBoolean("Running", true);
        legacy.remove("ForgeHeat");
        legacy.remove("LastForgeTick");
        legacy.remove("LastForgeAction");
        legacy.remove("LegacyForgeProgressPending");
        anvil.loadWithComponents(legacy, h.getLevel().registryAccess());

        WorkshopBlockEntity.serverTick(h.getLevel(), anvil.getBlockPos(), anvil.getBlockState(), anvil);
        check(h, anvil.getProgress() == 3, "Legacy 6/8 anvil progress did not migrate to 3/4 forge actions");
        check(h, anvil.getForgeHeat() == 0, "Legacy anvil migration invented heat");

        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STONE_HAMMER.get()));
        h.setBlock(pos.south(), Blocks.CAMPFIRE);
        check(h, anvil.reheat(player), "Migrated legacy billet could not be reheated");
        check(h, anvil.forge(player, "flatten"), "Migrated legacy billet could not finish its remaining forge action");
        check(h, anvil.getOutput().is(Items.COPPER_INGOT), "Migrated legacy anvil produced the wrong result");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void zonesRotateAndRejectSideFaces(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var state = ModBlocks.STONE_ANVIL.get().defaultBlockState().setValue(WorkshopBlock.FACING, facing);
            for (String action : new String[]{"flatten", "draw", "bend"}) {
                double x = action.equals("draw") ? 0.18 : 0.5;
                double z = action.equals("bend") ? 0.1 : 0.5;
                Vec3 local = new Vec3(x - 0.5, 0.6625, z - 0.5);
                Vec3 world = switch (facing) {
                    case EAST -> new Vec3(-local.z, local.y, local.x);
                    case SOUTH -> new Vec3(-local.x, local.y, -local.z);
                    case WEST -> new Vec3(local.z, local.y, -local.x);
                    default -> local;
                };
                world = world.add(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                check(h, StoneAnvilBlock.actionAt(state, pos, new BlockHitResult(world, Direction.UP, pos, false)).equals(action), "Rotated zone mismatch: " + facing + " " + action);
                check(h, StoneAnvilBlock.actionAt(state, pos, new BlockHitResult(world, facing, pos, false)).equals("none"), "Side face became forge zone");
            }
        }
        h.succeed();
    }
    private static void check(GameTestHelper h, boolean value, String message) { h.assertTrue(value, message); }
}
