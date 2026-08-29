package com.nstut.firstworks.content;

import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModFluids;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** A fragile, low-temperature earthenware bucket that accepts only Water and Tannin Solution. */
public final class ClayBucketItem extends BucketItem {
    public ClayBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (content == Fluids.EMPTY) {
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            Fluid target = hit.getType() == HitResult.Type.BLOCK
                    ? level.getFluidState(hit.getBlockPos()).getType()
                    : Fluids.EMPTY;
            if (!Fluids.WATER.isSame(target) && !ModFluids.TANNIN_SOLUTION.get().isSame(target)) {
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
        }

        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        ItemStack returned = result.getObject();
        if (content == Fluids.EMPTY && returned.is(Items.WATER_BUCKET)) {
            return new InteractionResultHolder<>(result.getResult(), new ItemStack(ModItems.WATER_CLAY_BUCKET.get()));
        }
        if (content == Fluids.EMPTY && returned.is(ModItems.TANNIN_SOLUTION_BUCKET.get())) {
            return new InteractionResultHolder<>(result.getResult(), new ItemStack(ModItems.TANNIN_CLAY_BUCKET.get()));
        }
        if ((content == Fluids.WATER || content == ModFluids.TANNIN_SOLUTION.get()) && returned.is(Items.BUCKET)) {
            return new InteractionResultHolder<>(result.getResult(), new ItemStack(ModItems.CLAY_BUCKET.get()));
        }
        return result;
    }
}
