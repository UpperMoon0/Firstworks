package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.content.loom.LoomBlockEntity;
import com.nstut.firstworks.content.loom.LoomRecipe;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class LoomWeavingKubeEvent implements KubeEvent {
    private final ServerLevel level;
    private final LoomBlockEntity loom;
    private final ResourceLocation recipeId;
    private final LoomRecipe recipe;
    private final ItemStack input;
    private final ItemStack result;

    public LoomWeavingKubeEvent(ServerLevel level, LoomBlockEntity loom, ResourceLocation recipeId,
            LoomRecipe recipe, ItemStack input, ItemStack result) {
        this.level = level;
        this.loom = loom;
        this.recipeId = recipeId;
        this.recipe = recipe;
        this.input = input.copy();
        this.result = result.copy();
    }

    public ServerLevel getLevel() { return level; }
    public BlockPos getPos() { return loom.getBlockPos(); }
    public LoomBlockEntity getLoom() { return loom; }
    public ResourceLocation getRecipeId() { return recipeId; }
    public LoomRecipe getRecipe() { return recipe; }
    public ItemStack getInput() { return input.copy(); }
    public ItemStack getResult() { return result.copy(); }
}
