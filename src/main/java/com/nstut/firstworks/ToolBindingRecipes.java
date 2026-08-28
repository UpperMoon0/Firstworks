package com.nstut.firstworks;

import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Firstworks.MOD_ID)
public final class ToolBindingRecipes {
    @SubscribeEvent
    public static void bindVanillaTools(OnDatapackSyncEvent event) {
        boolean bindTools = FirstworksConfig.BIND_VANILLA_TOOL_RECIPES.getAsBoolean();
        boolean bindPrimitive = bindTools && FirstworksConfig.BIND_PRIMITIVE_VANILLA_TOOLS.getAsBoolean();
        boolean bindMetal = bindTools && FirstworksConfig.BIND_METAL_VANILLA_TOOLS.getAsBoolean();
        boolean textiles = FirstworksConfig.ENABLE_TEXTILE_PROGRESSION.getAsBoolean();
        boolean masonry = FirstworksConfig.ENABLE_MASONRY_PROGRESSION.getAsBoolean();
        boolean replaceLeather = FirstworksConfig.REPLACE_ANIMAL_LEATHER.getAsBoolean();
        rewrite(event.getPlayerList().getServer().getRecipeManager(), bindPrimitive, bindMetal, textiles, masonry, replaceLeather);
    }

    private static void rewrite(RecipeManager manager, boolean bindPrimitive, boolean bindMetal, boolean textiles, boolean masonry, boolean replaceLeather) {
        Ingredient primitiveBinding = Ingredient.of(ModTags.PRIMITIVE_BINDINGS);
        Ingredient rope = Ingredient.of(ModTags.STRONG_BINDINGS);
        Map<ResourceLocation, Recipe<?>> replacements = new HashMap<>();

        if (bindPrimitive) {
            addTier(replacements, "wooden", Ingredient.of(ItemTags.PLANKS), primitiveBinding,
                    Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE, Items.WOODEN_SWORD);
            addTier(replacements, "stone", Ingredient.of(ItemTags.STONE_TOOL_MATERIALS), primitiveBinding,
                    Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL, Items.STONE_HOE, Items.STONE_SWORD);
        }
        if (bindMetal) {
            addTier(replacements, "iron", Ingredient.of(Items.IRON_INGOT), rope,
                    Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD);
            addTier(replacements, "golden", Ingredient.of(Items.GOLD_INGOT), rope,
                    Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE, Items.GOLDEN_SWORD);
            addTier(replacements, "diamond", Ingredient.of(Items.DIAMOND), rope,
                    Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_SWORD);
        }

        if (replaceLeather) {
            replacements.put(vanilla("leather"), shapedSimple(ModItems.RAW_HIDE.get(), Ingredient.of(Items.RABBIT_HIDE), "##", "##"));
        }

        List<RecipeHolder<?>> rewritten = new ArrayList<>(manager.getRecipes().size());
        int changed = 0;
        int removed = 0;
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (textiles && isVanillaTextileRecipe(holder.id())) {
                removed++;
                continue;
            }
            if (!textiles && isFirstworksTextileRecipe(holder.id())) {
                removed++;
                continue;
            }
            if (masonry && isVanillaMasonryRecipe(holder.id())) {
                removed++;
                continue;
            }
            if (!masonry && isFirstworksMasonryRecipe(holder.id())) {
                removed++;
                continue;
            }
            Recipe<?> replacement = replacements.get(holder.id());
            if (replacement == null) {
                rewritten.add(holder);
            } else {
                rewritten.add(new RecipeHolder<>(holder.id(), replacement));
                changed++;
            }
        }
        if (changed > 0 || removed > 0) {
            manager.replaceRecipes(rewritten);
            Firstworks.LOGGER.info("Reworked {} tool recipes and removed {} bypass textile recipes", changed, removed);
        }
    }

    private static final Set<String> BED_RECIPES = Set.of(
            "white_bed", "orange_bed", "magenta_bed", "light_blue_bed", "yellow_bed", "lime_bed",
            "pink_bed", "gray_bed", "light_gray_bed", "cyan_bed", "purple_bed", "blue_bed",
            "brown_bed", "green_bed", "red_bed", "black_bed");

    private static boolean isVanillaTextileRecipe(ResourceLocation id) {
        return id.getNamespace().equals("minecraft")
                && (id.getPath().equals("white_wool_from_string") || BED_RECIPES.contains(id.getPath()));
    }

    private static boolean isFirstworksTextileRecipe(ResourceLocation id) {
        return id.getNamespace().equals(Firstworks.MOD_ID) && Set.of(
                "wash_raw_fleece", "fleece_dyeing", "wool_block", "textile_bed").contains(id.getPath());
    }

    private static boolean isVanillaMasonryRecipe(ResourceLocation id) {
        return id.getNamespace().equals("minecraft") && Set.of(
                "brick", "brick_from_blasting", "bricks").contains(id.getPath());
    }

    private static boolean isFirstworksMasonryRecipe(ResourceLocation id) {
        return id.getNamespace().equals(Firstworks.MOD_ID) && Set.of(
                "brick_mold", "mold_unfired_clay_brick", "fire_clay_brick", "fire_clay_brick_from_smelting",
                "mix_mortar", "mortar_bound_brick_block").contains(id.getPath());
    }

    private static void addTier(Map<ResourceLocation, Recipe<?>> recipes, String tier, Ingredient material,
            Ingredient binding, Item pickaxe, Item axe, Item shovel, Item hoe, Item sword) {
        recipes.put(vanilla(tier + "_pickaxe"), shaped(pickaxe, material, binding, "MMM", "BS ", " S "));
        recipes.put(vanilla(tier + "_axe"), shaped(axe, material, binding, "MM ", "MSB", " S "));
        recipes.put(vanilla(tier + "_shovel"), shaped(shovel, material, binding, " M ", "BS ", " S "));
        recipes.put(vanilla(tier + "_hoe"), shaped(hoe, material, binding, "MM ", "BS ", " S "));
        recipes.put(vanilla(tier + "_sword"), shaped(sword, material, binding, " M ", " M ", "BS "));
    }

    private static ShapedRecipe shaped(Item result, Ingredient material, Ingredient binding, String... pattern) {
        Map<Character, Ingredient> keys = Map.of(
                'M', material,
                'S', Ingredient.of(Items.STICK),
                'B', binding);
        return new ShapedRecipe("", CraftingBookCategory.EQUIPMENT,
                ShapedRecipePattern.of(keys, pattern), new ItemStack(result));
    }

    private static ShapedRecipe shapedSimple(Item result, Ingredient material, String... pattern) {
        Map<Character, Ingredient> keys = Map.of('#', material);
        return new ShapedRecipe("", CraftingBookCategory.MISC,
                ShapedRecipePattern.of(keys, pattern), new ItemStack(result));
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private ToolBindingRecipes() {}
}
