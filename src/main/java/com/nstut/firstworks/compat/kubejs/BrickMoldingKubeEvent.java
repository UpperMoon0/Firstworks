package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.content.brick_mold.BrickMoldBlockEntity;
import com.nstut.firstworks.content.brick_mold.BrickMoldingRecipe;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class BrickMoldingKubeEvent implements KubeEvent {
    private final ServerLevel level;
    private final BrickMoldBlockEntity mold;
    private final ResourceLocation recipeId;
    private final BrickMoldingRecipe recipe;
    private final ItemStack input;
    private final ItemStack result;

    public BrickMoldingKubeEvent(ServerLevel level, BrickMoldBlockEntity mold, ResourceLocation recipeId,
            BrickMoldingRecipe recipe, ItemStack input, ItemStack result) {
        this.level = level;
        this.mold = mold;
        this.recipeId = recipeId;
        this.recipe = recipe;
        this.input = input.copy();
        this.result = result.copy();
    }

    public ServerLevel getLevel() { return level; }
    public BlockPos getPos() { return mold.getBlockPos(); }
    public BrickMoldBlockEntity getMold() { return mold; }
    public ResourceLocation getRecipeId() { return recipeId; }
    public BrickMoldingRecipe getRecipe() { return recipe; }
    public ItemStack getInput() { return input.copy(); }
    public ItemStack getResult() { return result.copy(); }
}
