package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModRecipes;
import com.nstut.firstworks.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

@JeiPlugin
public final class FirstworksJeiPlugin implements IModPlugin {
    public static final RecipeType<BarrelRecipe> BARREL_PROCESSING =
            RecipeType.create(Firstworks.MOD_ID, "barrel_processing", BarrelRecipe.class);
    private static final ResourceLocation UID = Firstworks.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new BarrelRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(ModItems.TREE_BARK.get(),
                Component.translatable("jei.firstworks.tree_bark.obtain"));
        if (Minecraft.getInstance().level == null) return;
        var recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.BARREL_PROCESSING_TYPE.get())
                .stream()
                .map(holder -> holder.value())
                .toList();
        registration.addRecipes(BARREL_PROCESSING, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ModBlocks.BARRELS.values().forEach(barrel ->
                registration.addRecipeCatalyst(barrel.get(), BARREL_PROCESSING));
    }
}
