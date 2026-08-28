package com.nstut.firstworks.content;

import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class MortarAndPestleItem extends Item {
    private static final int MAX_USE_DURATION = 72_000;

    public MortarAndPestleItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack mortar = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || findRecipe(level, player.getOffhandItem()).isEmpty()) {
            return InteractionResultHolder.fail(mortar);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(mortar);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;
        Optional<RecipeHolder<MortarGrindingRecipe>> active = findRecipe(level, player.getOffhandItem());
        if (active.isEmpty()) { player.stopUsingItem(); return; }
        int elapsed = MAX_USE_DURATION - remainingUseDuration;
        if (elapsed > 0 && elapsed % 10 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRINDSTONE_USE,
                    SoundSource.PLAYERS, 0.35F, 0.8F);
            if (level instanceof ServerLevel serverLevel) serverLevel.sendParticles(ParticleTypes.POOF,
                    player.getX(), player.getEyeY() - 0.45, player.getZ(), 1, 0.08, 0.04, 0.08, 0.005);
        }
        MortarGrindingRecipe recipe = active.get().value();
        if (elapsed < Math.max(1, recipe.duration())) return;
        if (!level.isClientSide) complete(player, recipe);
        player.stopUsingItem();
    }

    private static void complete(Player player, MortarGrindingRecipe recipe) {
        ItemStack input = player.getOffhandItem();
        if (!player.hasInfiniteMaterials()) input.shrink(recipe.inputCount());
        player.getInventory().placeItemBackInInventory(recipe.result().copy());
        ItemStack mortar = player.getMainHandItem();
        player.awardStat(Stats.ITEM_USED.get(mortar.getItem()));
        if (!player.hasInfiniteMaterials()) mortar.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    private static Optional<RecipeHolder<MortarGrindingRecipe>> findRecipe(Level level, ItemStack input) {
        if (input.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(ModRecipes.MORTAR_GRINDING_TYPE.get(),
                new SingleRecipeInput(input), level);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return MAX_USE_DURATION; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.firstworks.mortar_and_pestle.use").withStyle(ChatFormatting.GRAY));
    }
}
