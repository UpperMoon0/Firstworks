package com.nstut.firstworks.compat.jei;

import com.nstut.firstworks.content.brick_mold.BrickMoldingRecipe;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.content.loom.LoomRecipe;
import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.content.TextileColors;
import com.nstut.firstworks.content.SpinningRecipe;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModRecipes;
import com.nstut.firstworks.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JeiPlugin
public final class FirstworksJeiPlugin implements IModPlugin {
    public static final RecipeType<BarrelRecipe> BARREL_PROCESSING =
            RecipeType.create(Firstworks.MOD_ID, "barrel_processing", BarrelRecipe.class);
    public static final RecipeType<LoomRecipe> LOOM_WEAVING =
            RecipeType.create(Firstworks.MOD_ID, "loom_weaving", LoomRecipe.class);
    public static final RecipeType<SpinningRecipe> SPINDLE_SPINNING =
            RecipeType.create(Firstworks.MOD_ID, "spinning", SpinningRecipe.class);
    public static final RecipeType<BrickMoldingRecipe> BRICK_MOLDING =
            RecipeType.create(Firstworks.MOD_ID, "brick_molding", BrickMoldingRecipe.class);
    private static final ResourceLocation UID = Firstworks.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new BarrelRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new LoomRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new SpinningRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new BrickMoldingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(ModItems.TREE_BARK.get(),
                Component.translatable("jei.firstworks.tree_bark.obtain"));
        registration.addIngredientInfo(ModItems.PLANT_FIBRE.get(),
                Component.translatable("jei.firstworks.plant_fibre.obtain"));
        registration.addIngredientInfo(ModItems.RAW_FLEECE.get(),
                Component.translatable("jei.firstworks.raw_fleece.use"));
        if (Minecraft.getInstance().level == null) return;
        var recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.BARREL_PROCESSING_TYPE.get())
                .stream()
                .map(holder -> holder.value())
                .toList();
        registration.addRecipes(BARREL_PROCESSING, recipes);
        var loomRecipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.LOOM_WEAVING_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        registration.addRecipes(LOOM_WEAVING, loomRecipes);
        var spinningRecipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.SPINNING_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        registration.addRecipes(SPINDLE_SPINNING, spinningRecipes);
        var brickMoldingRecipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.BRICK_MOLDING_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        registration.addRecipes(BRICK_MOLDING, brickMoldingRecipes);

        var manager = Minecraft.getInstance().level.getRecipeManager();
        List<RecipeHolder<CraftingRecipe>> textileDisplays = new ArrayList<>();
        if (manager.byKey(Firstworks.id("wool_block")).isPresent()) {
            addWoolDisplays(textileDisplays);
        }
        if (manager.byKey(Firstworks.id("textile_bed")).isPresent()) {
            addBedDisplays(textileDisplays);
        }
        registration.addRecipes(RecipeTypes.CRAFTING, textileDisplays);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ModBlocks.BARRELS.values().forEach(barrel ->
                registration.addRecipeCatalyst(barrel.get(), BARREL_PROCESSING));
        ModBlocks.LOOMS.values().forEach(loom ->
                registration.addRecipeCatalyst(loom.get(), LOOM_WEAVING));
        registration.addRecipeCatalyst(ModItems.HAND_SPINDLE.get(), SPINDLE_SPINNING);
        registration.addRecipeCatalyst(ModBlocks.BRICK_MOLD.get(), BRICK_MOLDING);
    }

    static List<ItemStack> fleeceVariants(Item item, int count) {
        return Arrays.stream(DyeColor.values())
                .map(color -> ColoredFleeceItem.create(item, color, count))
                .toList();
    }

    private static void addWoolDisplays(List<RecipeHolder<CraftingRecipe>> recipes) {
        for (DyeColor color : DyeColor.values()) {
            Ingredient wool = exactFleece(ModItems.CLEAN_WOOL.get(), color);
            ShapedRecipe recipe = new ShapedRecipe("", CraftingBookCategory.BUILDING,
                    ShapedRecipePattern.of(Map.of('W', wool), "WW", "WW"),
                    new ItemStack(TextileColors.wool(color)));
            recipes.add(new RecipeHolder<>(Firstworks.id("jei/wool_block_" + color.getName()), recipe));
        }
    }

    private static void addBedDisplays(List<RecipeHolder<CraftingRecipe>> recipes) {
        for (DyeColor color : DyeColor.values()) {
            Map<Character, Ingredient> keys = Map.of(
                    'C', Ingredient.of(ModItems.CLOTH.get()),
                    'W', exactFleece(ModItems.CLEAN_WOOL.get(), color),
                    'P', Ingredient.of(ItemTags.PLANKS));
            ShapedRecipe recipe = new ShapedRecipe("", CraftingBookCategory.MISC,
                    ShapedRecipePattern.of(keys, "CCC", "WWW", "PPP"),
                    new ItemStack(TextileColors.bed(color)));
            recipes.add(new RecipeHolder<>(Firstworks.id("jei/textile_bed_" + color.getName()), recipe));
        }
    }

    private static Ingredient exactFleece(Item item, DyeColor color) {
        return DataComponentIngredient.of(true, ColoredFleeceItem.create(item, color, 1));
    }
}
