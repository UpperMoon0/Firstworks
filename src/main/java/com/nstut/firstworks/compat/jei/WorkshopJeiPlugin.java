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

import java.util.List;

@JeiPlugin
public final class WorkshopJeiPlugin implements IModPlugin {
    public static final RecipeType<WorkshopRecipe> POTTERY_WHEEL_PROCESSING =
            RecipeType.create(Firstworks.MOD_ID, "pottery_wheel", WorkshopRecipe.class);
    public static final RecipeType<WorkshopRecipe> KILN_PROCESSING =
            RecipeType.create(Firstworks.MOD_ID, "kiln", WorkshopRecipe.class);
    public static final RecipeType<WorkshopRecipe> STONE_ANVIL_PROCESSING =
            RecipeType.create(Firstworks.MOD_ID, "stone_anvil", WorkshopRecipe.class);
    public static final RecipeType<WorkshopRecipe> CRUCIBLE_FURNACE_PROCESSING =
            RecipeType.create(Firstworks.MOD_ID, "crucible_furnace", WorkshopRecipe.class);
    private static final ResourceLocation UID = Firstworks.id("workshop_jei_plugin");

    @Override public ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new WorkshopRecipeCategory(guiHelper, POTTERY_WHEEL_PROCESSING, WorkshopRecipe.POTTERY_WHEEL),
                new WorkshopRecipeCategory(guiHelper, KILN_PROCESSING, WorkshopRecipe.KILN),
                new WorkshopRecipeCategory(guiHelper, STONE_ANVIL_PROCESSING, WorkshopRecipe.STONE_ANVIL),
                new WorkshopRecipeCategory(guiHelper, CRUCIBLE_FURNACE_PROCESSING, WorkshopRecipe.CRUCIBLE_FURNACE));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        List<WorkshopRecipe> recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.WORKSHOP_PROCESSING_TYPE.get()).stream()
                .map(holder -> holder.value()).toList();
        registerStationRecipes(registration, recipes, POTTERY_WHEEL_PROCESSING, WorkshopRecipe.POTTERY_WHEEL);
        registerStationRecipes(registration, recipes, KILN_PROCESSING, WorkshopRecipe.KILN);
        registerStationRecipes(registration, recipes, STONE_ANVIL_PROCESSING, WorkshopRecipe.STONE_ANVIL);
        registerStationRecipes(registration, recipes, CRUCIBLE_FURNACE_PROCESSING, WorkshopRecipe.CRUCIBLE_FURNACE);
    }

    private static void registerStationRecipes(IRecipeRegistration registration, List<WorkshopRecipe> recipes,
                                               RecipeType<WorkshopRecipe> recipeType, String station) {
        registration.addRecipes(recipeType, recipes.stream()
                .filter(recipe -> station.equals(recipe.station()))
                .toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.POTTERY_WHEEL.get(), POTTERY_WHEEL_PROCESSING);
        registration.addRecipeCatalyst(ModBlocks.KILN.get(), KILN_PROCESSING);
        registration.addRecipeCatalyst(ModBlocks.STONE_ANVIL.get(), STONE_ANVIL_PROCESSING);
        registration.addRecipeCatalyst(ModBlocks.CRUCIBLE_FURNACE.get(), CRUCIBLE_FURNACE_PROCESSING);
        registration.addRecipeCatalyst(ModBlocks.BELLOWS.get(), CRUCIBLE_FURNACE_PROCESSING);
    }
}
