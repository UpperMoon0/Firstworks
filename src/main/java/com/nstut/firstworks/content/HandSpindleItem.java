package com.nstut.firstworks.content;

import com.nstut.firstworks.compat.OptionalIntegrations;
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

public class HandSpindleItem extends Item {
    private static final int MAX_USE_DURATION = 72_000;
    private final float durationScale;

    public HandSpindleItem(Properties properties) { this(properties, 1.0F); }
    public HandSpindleItem(Properties properties, float durationScale) {
        super(properties.stacksTo(1));
        this.durationScale = Math.max(0.1F, durationScale);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack spindle = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.fail(spindle);
        Optional<RecipeHolder<SpinningRecipe>> recipe = findRecipe(level, player.getOffhandItem());
        if (recipe.isEmpty()) return InteractionResultHolder.fail(spindle);
        RecipeHolder<SpinningRecipe> holder = recipe.get();
        if (level instanceof ServerLevel serverLevel && OptionalIntegrations.fireSpindleSpinningStarting(serverLevel, player, holder.id(), holder.value(), player.getOffhandItem().copyWithCount(holder.value().inputCount()), holder.value().result())) return InteractionResultHolder.fail(spindle);
        player.startUsingItem(hand); return InteractionResultHolder.consume(spindle);
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) return;
        Optional<RecipeHolder<SpinningRecipe>> active = findRecipe(level, player.getOffhandItem());
        if (active.isEmpty()) { player.stopUsingItem(); return; }
        int elapsed = MAX_USE_DURATION - remainingUseDuration;
        if (elapsed > 0 && elapsed % 8 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.PLAYERS, 0.35F, 1.25F);
            if (level instanceof ServerLevel serverLevel) serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getEyeY() - 0.45, player.getZ(), 1, 0.08, 0.08, 0.08, 0.005);
        }
        RecipeHolder<SpinningRecipe> holder = active.get();
        int required = Math.max(1, Math.round(holder.value().duration() * durationScale));
        if (elapsed < required) return;
        if (!level.isClientSide) complete((ServerLevel) level, player, holder);
        player.stopUsingItem();
    }

    private static void complete(ServerLevel level, Player player, RecipeHolder<SpinningRecipe> holder) {
        SpinningRecipe recipe = holder.value(); ItemStack input = player.getOffhandItem(); ItemStack consumed = input.copyWithCount(recipe.inputCount());
        if (!player.hasInfiniteMaterials()) input.shrink(recipe.inputCount()); ItemStack result = recipe.result().copy();
        player.getInventory().placeItemBackInInventory(result.copy()); level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.PLAYERS, 0.75F, 1.1F);
        ItemStack spindle = player.getMainHandItem(); player.awardStat(Stats.ITEM_USED.get(spindle.getItem())); OptionalIntegrations.fireSpindleSpinningCompleted(level, player, holder.id(), recipe, consumed, result);
        if (!player.hasInfiniteMaterials()) spindle.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }
    private static Optional<RecipeHolder<SpinningRecipe>> findRecipe(Level level, ItemStack input) { if (input.isEmpty()) return Optional.empty(); return level.getRecipeManager().getRecipeFor(ModRecipes.SPINNING_TYPE.get(), new SingleRecipeInput(input), level); }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return MAX_USE_DURATION; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) { tooltip.add(Component.translatable("tooltip.firstworks.hand_spindle.use").withStyle(ChatFormatting.GRAY)); }
}
