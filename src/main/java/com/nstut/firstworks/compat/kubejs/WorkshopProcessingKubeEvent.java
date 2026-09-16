package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class WorkshopProcessingKubeEvent implements KubeEvent {
    private final ServerLevel level;
    private final WorkshopBlockEntity workshop;
    private final String station;
    private final ResourceLocation recipeId;
    private final WorkshopRecipe recipe;
    private final ItemStack input;
    private final ItemStack catalyst;
    private final ItemStack result;

    public WorkshopProcessingKubeEvent(ServerLevel level, WorkshopBlockEntity workshop, String station,
            ResourceLocation recipeId, WorkshopRecipe recipe, ItemStack input, ItemStack catalyst, ItemStack result) {
        this.level = level;
        this.workshop = workshop;
        this.station = station;
        this.recipeId = recipeId;
        this.recipe = recipe;
        this.input = input.copy();
        this.catalyst = catalyst.copy();
        this.result = result.copy();
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return workshop.getBlockPos();
    }

    public WorkshopBlockEntity getWorkshop() {
        return workshop;
    }

    public String getStation() {
        return station;
    }

    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    public WorkshopRecipe getRecipe() {
        return recipe;
    }

    public ItemStack getInput() {
        return input.copy();
    }

    public ItemStack getCatalyst() {
        return catalyst.copy();
    }

    public ItemStack getResult() {
        return result.copy();
    }
}
