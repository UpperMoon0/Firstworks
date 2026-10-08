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
                && !p.getInventory().contains(new ItemStack(Items.LAVA_BUCKET)),
                "Creative refill created extra items");
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
