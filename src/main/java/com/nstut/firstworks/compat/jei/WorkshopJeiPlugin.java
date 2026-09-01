package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class WorkshopJeiPlugin implements IModPlugin {
    public static final RecipeType<WorkshopRecipe> WORKSHOP_PROCESSING =
            RecipeType.create(Firstworks.MOD_ID, "workshop_processing", WorkshopRecipe.class);
    private static final ResourceLocation UID = Firstworks.id("workshop_jei_plugin");

    @Override public ResourceLocation getPluginUid() { return UID; }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new WorkshopRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        var recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.WORKSHOP_PROCESSING_TYPE.get()).stream()
                .map(holder -> holder.value()).toList();
        registration.addRecipes(WORKSHOP_PROCESSING, recipes);
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.POTTERY_WHEEL.get(), WORKSHOP_PROCESSING);
        registration.addRecipeCatalyst(ModBlocks.KILN.get(), WORKSHOP_PROCESSING);
        registration.addRecipeCatalyst(ModBlocks.STONE_ANVIL.get(), WORKSHOP_PROCESSING);
        registration.addRecipeCatalyst(ModBlocks.CRUCIBLE_FURNACE.get(), WORKSHOP_PROCESSING);
        registration.addRecipeCatalyst(ModBlocks.BELLOWS.get(), WORKSHOP_PROCESSING);
    }
}
