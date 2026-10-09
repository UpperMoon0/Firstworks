package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelBlock;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModFluids;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BarrelContainerGameTests {
    private static BarrelBlockEntity barrel(GameTestHelper h) {
        h.setBlock(new BlockPos(3, 1, 3), ModBlocks.BARREL.get());
        return h.getBlockEntity(new BlockPos(3, 1, 3));
    }
    private static void use(GameTestHelper h, BarrelBlockEntity b, Player p, InteractionHand hand) {
        var pos = b.getBlockPos();
        b.getBlockState().useItemOn(p.getItemInHand(hand), h.getLevel(), p, hand,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, false));
    }

    // Reproduce the normal main-hand item -> block fallback -> offhand item order.
    // Calling useItemOn(OFF_HAND) directly misses the main-hand fallback regression.
    private static void useLikeVanilla(GameTestHelper h, BarrelBlockEntity b, Player p) {
        var pos = b.getBlockPos();
        var level = h.getLevel();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, false);
        for (var hand : InteractionHand.values()) {
            var state = level.getBlockState(pos);
            ItemInteractionResult itemResult = state.useItemOn(p.getItemInHand(hand), level, p, hand, hit);
            if (itemResult.consumesAction()) return;
            if (itemResult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                InteractionResult defaultResult = level.getBlockState(pos).useWithoutItem(level, p, hit);
                if (defaultResult.consumesAction()) return;
            }
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void vanillaAndModdedFluidsUseHandContainersAndOutputFirstDrain(GameTestHelper h) {
        var b = barrel(h);
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        for (var fluid : List.of(Fluids.WATER, Fluids.LAVA, ModFluids.TANNIN_SOLUTION.get())) {
            var filled = FluidUtil.getFilledBucket(new FluidStack(fluid, 1000));
            h.assertTrue(!filled.isEmpty(), "Missing fluid bucket");
            p.setItemInHand(InteractionHand.OFF_HAND, filled);
            use(h, b, p, InteractionHand.OFF_HAND);
            h.assertTrue(b.getInputTank().getFluidAmount() == 1000
                    && b.getInputTank().getFluid().is(fluid) && p.getOffhandItem().is(Items.BUCKET),
                    "Generic offhand pour failed");
            use(h, b, p, InteractionHand.OFF_HAND);
            h.assertTrue(b.getTotalFluidAmount() == 0
                    && FluidUtil.getFluidContained(p.getOffhandItem()).orElseThrow().is(fluid),
                    "Generic offhand refill failed");
        }
        b.addInputWater(1000);
        b.getOutputTank().fill(new FluidStack(ModFluids.TANNIN_SOLUTION.get(), 1000), FluidAction.EXECUTE);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 2));
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getOutputTank().isEmpty() && b.getInputTank().getFluidAmount() == 1000
                && p.getMainHandItem().getCount() == 1
                && p.getInventory().contains(new ItemStack(ModItems.TANNIN_SOLUTION_BUCKET.get())),
                "Output priority or stacked-container inventory return failed");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void offhandContainerSurvivesMainhandFallback(GameTestHelper h) {
        var b = barrel(h);
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        b.addInputWater(1000);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
        useLikeVanilla(h, b, p);
        h.assertTrue(!b.getBlockState().getValue(BarrelBlock.SEALED)
                && b.getTotalFluidAmount() == 0 && p.getOffhandItem().is(Items.WATER_BUCKET),
                "Empty main hand toggled lid instead of reaching offhand bucket");

        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
        useLikeVanilla(h, b, p);
        h.assertTrue(!b.getBlockState().getValue(BarrelBlock.SEALED)
                && b.getInputTank().getFluidAmount() == 1000
                && p.getOffhandItem().is(Items.BUCKET) && p.getMainHandItem().is(Items.FLINT),
                "Unrelated main hand intercepted offhand fluid pour");

        h.getLevel().setBlock(b.getBlockPos(), b.getBlockState().setValue(BarrelBlock.SEALED, true), 3);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        useLikeVanilla(h, b, p);
        h.assertTrue(b.getBlockState().getValue(BarrelBlock.SEALED)
                && b.getInputTank().getFluidAmount() == 1000 && p.getOffhandItem().is(Items.BUCKET),
                "Sealed barrel was opened or drained before the offhand could be rejected");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void sneakUseInsertsFluidCapableRecipeIngredient(GameTestHelper h) {
        var b = barrel(h);
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        h.assertTrue(b.canInsert(p.getMainHandItem()), "Test datapack fluid-container recipe is missing");
        p.setShiftKeyDown(true);
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getIngredient().is(Items.WATER_BUCKET)
                && p.getMainHandItem().isEmpty() && b.getTotalFluidAmount() == 0,
                "Sneak-use poured a recipe ingredient instead of inserting it");

        p.setShiftKeyDown(false);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getIngredient().is(Items.WATER_BUCKET)
                && b.getInputTank().getFluidAmount() == 1000 && p.getMainHandItem().is(Items.BUCKET),
                "Normal use failed to transfer fluid when the container matched a recipe");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void failedSealedAndCreativeTransfersPreserveItemsAndCapacity(GameTestHelper h) {
        var b = barrel(h);
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        b.addInputWater(3500);
        var filled = new ItemStack(Items.WATER_BUCKET);
        p.setItemInHand(InteractionHand.MAIN_HAND, filled);
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == 3500 && p.getMainHandItem().is(Items.WATER_BUCKET),
                "Insufficient capacity consumed a bucket");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == 3500 && p.getMainHandItem().is(Items.LAVA_BUCKET),
                "Incompatible fluid was mixed or consumed");
        h.getLevel().setBlock(b.getBlockPos(), b.getBlockState().setValue(BarrelBlock.SEALED, true), 3);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == 3500 && p.getMainHandItem().is(Items.BUCKET),
                "Sealed barrel transferred");
        h.getLevel().setBlock(b.getBlockPos(), b.getBlockState().setValue(BarrelBlock.SEALED, false), 3);
        b.getInputTank().drain(4000, FluidAction.EXECUTE);
        b.addInputWater(250);
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == 250 && p.getMainHandItem().is(Items.BUCKET),
                "Partial bucket drainage destroyed fluid");
        b.getInputTank().drain(4000, FluidAction.EXECUTE);
        p.getAbilities().instabuild = true;
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == 1000 && p.getMainHandItem().is(Items.LAVA_BUCKET),
                "Creative pour consumed container");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == 0 && p.getMainHandItem().is(Items.BUCKET)
                && p.getInventory().contains(new ItemStack(Items.LAVA_BUCKET)),
                "Creative refill lost fluid without returning a container");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void clayRestrictionsAndWaterBottleRemainSafe(GameTestHelper h) {
        var b = barrel(h);
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        b.addInputFluid(new FluidStack(Fluids.LAVA, 1000));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CLAY_BUCKET.get()));
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == 1000 && p.getMainHandItem().is(ModItems.CLAY_BUCKET.get()),
                "Clay bucket accepted lava");
        b.getInputTank().drain(4000, FluidAction.EXECUTE);
        var bottle = new ItemStack(Items.POTION);
        bottle.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.WATER));
        p.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == 250
                && p.getInventory().contains(new ItemStack(Items.GLASS_BOTTLE)), "Water bottle behavior changed");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WATER_CLAY_BUCKET.get()));
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == 1250
                && p.getInventory().contains(new ItemStack(ModItems.CLAY_BUCKET.get())), "Clay return changed");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void mixedStoresExposeNamesAmountsAndCreativeOutputReturn(GameTestHelper h) {
        var b = barrel(h);
        b.addInputWater(3000);
        b.getOutputTank().fill(new FluidStack(ModFluids.TANNIN_SOLUTION.get(), 1000), FluidAction.EXECUTE);
        var data = new net.minecraft.nbt.CompoundTag();
        com.nstut.firstworks.compat.jade.BarrelProgressProvider.appendFluidData(data, b);
        h.assertTrue(data.getInt("FirstworksInputAmount") == 3000
                && data.getInt("FirstworksOutputAmount") == 1000
                && data.getString("FirstworksInputName").equals(Fluids.WATER.getFluidType().getDescriptionId())
                && data.getString("FirstworksOutputName").equals(ModFluids.TANNIN_SOLUTION.get().getFluidType().getDescriptionId()),
                "Jade data hides mixed input/output stores");
        h.assertTrue(BarrelBlock.failedTransferReason(b, new ItemStack(Items.WATER_BUCKET)).equals("capacity"),
                "Full barrel feedback is misleading");
        h.assertTrue(BarrelBlock.failedTransferReason(b, new ItemStack(Items.LAVA_BUCKET)).equals("incompatible"),
                "Conflicting input feedback is misleading");
        var p = h.makeMockPlayer(GameType.CREATIVE);
        p.getAbilities().instabuild = true;
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET, 2));
        use(h, b, p, InteractionHand.OFF_HAND);
        h.assertTrue(b.getInputTank().getFluidAmount() == 3000 && b.getOutputTank().isEmpty()
                && p.getOffhandItem().getCount() == 2
                && p.getInventory().contains(new ItemStack(ModItems.TANNIN_SOLUTION_BUCKET.get())),
                "Creative offhand extraction failed to return output container");
        com.nstut.firstworks.compat.jade.BarrelProgressProvider.appendFluidData(data, b);
        h.assertTrue(data.getInt("FirstworksOutputAmount") == 0 && !data.contains("FirstworksOutputName"),
                "Jade retains stale output data");
        h.succeed();
    }

    @GameTestGenerator
    public static List<TestFunction> optionalBucketLibFixture() {
        if (!ModList.get().isLoaded("bucketlib")) return List.of();
        return List.of(new TestFunction("defaultBatch", "firstworks.bucketlibContainerRoundTrip",
                "firstworks:empty", 20, 0, true, BarrelContainerGameTests::bucketLibRoundTrip));
    }

    private static void bucketLibRoundTrip(GameTestHelper h) {
        var b = barrel(h);
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        for (var id : List.of("woodenbucket:wooden_bucket", "ceramicbucket:ceramic_bucket")) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            h.assertTrue(item != Items.AIR, "Optional compatibility fixture missing " + id);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            b.addInputFluid(new FluidStack(ModFluids.TANNIN_SOLUTION.get(), 1000));
            use(h, b, p, InteractionHand.MAIN_HAND);
            h.assertTrue(b.getTotalFluidAmount() == 0 && p.getMainHandItem().is(item)
                    && FluidUtil.getFluidContained(p.getMainHandItem()).orElseThrow().is(ModFluids.TANNIN_SOLUTION.get()),
                    "BucketLib refill failed for " + id);
            use(h, b, p, InteractionHand.MAIN_HAND);
            h.assertTrue(b.getTotalFluidAmount() == 1000 && p.getMainHandItem().is(item)
                    && FluidUtil.getFluidContained(p.getMainHandItem()).isEmpty(), "BucketLib pour failed for " + id);
            b.getInputTank().drain(4000, FluidAction.EXECUTE);
            p.getAbilities().instabuild = true;
            p.getInventory().clearContent();
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item));
            b.addInputFluid(new FluidStack(ModFluids.TANNIN_SOLUTION.get(), 1000));
            use(h, b, p, InteractionHand.OFF_HAND);
            h.assertTrue(b.getTotalFluidAmount() == 0
                    && FluidUtil.getFluidContained(p.getOffhandItem()).isEmpty()
                    && p.getInventory().items.stream().anyMatch(returned -> returned.is(item)
                        && FluidUtil.getFluidContained(returned).map(f -> f.is(ModFluids.TANNIN_SOLUTION.get())).orElse(false)),
                    "Creative BucketLib extraction lost fluid without a returned container: " + id);
            p.getAbilities().instabuild = false;
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item));
            b.addInputFluid(new FluidStack(ModFluids.TANNIN_SOLUTION.get(), 1000));
            useLikeVanilla(h, b, p);
            h.assertTrue(!b.getBlockState().getValue(BarrelBlock.SEALED)
                    && b.getTotalFluidAmount() == 0
                    && FluidUtil.getFluidContained(p.getOffhandItem())
                            .map(f -> f.is(ModFluids.TANNIN_SOLUTION.get())).orElse(false),
                    "Mainhand fallback intercepted offhand BucketLib container: " + id);
        }
        b.addInputFluid(new FluidStack(Fluids.LAVA, 1000));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse("woodenbucket:wooden_bucket"))));
        var simulated = FluidUtil.tryFillContainer(p.getMainHandItem(), b.getAutomationFluidHandler(),
                1000, null, false);
        h.assertTrue(b.getTotalFluidAmount() == 1000, "Container simulation mutated barrel");
        use(h, b, p, InteractionHand.MAIN_HAND);
        h.assertTrue(b.getTotalFluidAmount() == (simulated.isSuccess() ? 0 : 1000),
                "Container fluid eligibility was bypassed");
        if (simulated.isSuccess()) h.assertTrue(ItemStack.isSameItemSameComponents(
                p.getMainHandItem(), simulated.getResult()), "Container restriction/replacement result was ignored");
        h.succeed();
    }
}
