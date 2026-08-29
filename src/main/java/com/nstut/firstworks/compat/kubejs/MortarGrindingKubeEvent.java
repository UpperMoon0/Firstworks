package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.content.MortarGrindingRecipe;
import com.nstut.firstworks.content.mortar.MortarBlockEntity;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class MortarGrindingKubeEvent implements KubeEvent {
    private final ServerLevel level; private final MortarBlockEntity mortar; private final ResourceLocation recipeId;
    private final MortarGrindingRecipe recipe; private final ItemStack input; private final ItemStack result;
    public MortarGrindingKubeEvent(ServerLevel level, MortarBlockEntity mortar, ResourceLocation recipeId,
            MortarGrindingRecipe recipe, ItemStack input, ItemStack result) {
        this.level = level; this.mortar = mortar; this.recipeId = recipeId; this.recipe = recipe;
        this.input = input.copy(); this.result = result.copy();
    }
    public ServerLevel getLevel() { return level; }
    public BlockPos getPos() { return mortar.getBlockPos(); }
    public MortarBlockEntity getMortar() { return mortar; }
    public ResourceLocation getRecipeId() { return recipeId; }
    public MortarGrindingRecipe getRecipe() { return recipe; }
    public ItemStack getInput() { return input.copy(); }
    public ItemStack getResult() { return result.copy(); }
}
