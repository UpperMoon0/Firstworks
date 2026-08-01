package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class BarrelProcessKubeEvent implements KubeEvent {
    private final ServerLevel level;
    private final BarrelBlockEntity barrel;
    private final ResourceLocation recipeId;
    private final BarrelRecipe recipe;
    private final ItemStack input;
    private final FluidStack inputFluid;
    private final ItemStack result;
    private final FluidStack outputFluid;

    public BarrelProcessKubeEvent(ServerLevel level, BarrelBlockEntity barrel, ResourceLocation recipeId,
            BarrelRecipe recipe, ItemStack input, FluidStack inputFluid, ItemStack result, FluidStack outputFluid) {
        this.level = level;
        this.barrel = barrel;
        this.recipeId = recipeId;
        this.recipe = recipe;
        this.input = input.copy();
        this.inputFluid = inputFluid.copy();
        this.result = result.copy();
        this.outputFluid = outputFluid.copy();
    }

    public ServerLevel getLevel() { return level; }
    public BlockPos getPos() { return barrel.getBlockPos(); }
    public BarrelBlockEntity getBarrel() { return barrel; }
    public ResourceLocation getRecipeId() { return recipeId; }
    public BarrelRecipe getRecipe() { return recipe; }
    public ItemStack getInput() { return input.copy(); }
    public FluidStack getInputFluid() { return inputFluid.copy(); }
    public ItemStack getResult() { return result.copy(); }
    public FluidStack getOutputFluid() { return outputFluid.copy(); }
}
