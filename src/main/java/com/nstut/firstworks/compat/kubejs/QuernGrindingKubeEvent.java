package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.content.quern.QuernBlockEntity;
import com.nstut.firstworks.content.quern.QuernGrindingRecipe;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class QuernGrindingKubeEvent implements KubeEvent {
    private final ServerLevel level;
    private final QuernBlockEntity quern;
    private final ResourceLocation recipeId;
    private final QuernGrindingRecipe recipe;
    private final ItemStack input;
    private final ItemStack result;

    public QuernGrindingKubeEvent(ServerLevel level, QuernBlockEntity quern, ResourceLocation recipeId,
            QuernGrindingRecipe recipe, ItemStack input, ItemStack result) {
        this.level = level;
        this.quern = quern;
        this.recipeId = recipeId;
        this.recipe = recipe;
        this.input = input.copy();
        this.result = result.copy();
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return quern.getBlockPos();
    }

    public QuernBlockEntity getQuern() {
        return quern;
    }

    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    public QuernGrindingRecipe getRecipe() {
        return recipe;
    }

    public ItemStack getInput() {
        return input.copy();
    }

    public ItemStack getResult() {
        return result.copy();
    }
}
