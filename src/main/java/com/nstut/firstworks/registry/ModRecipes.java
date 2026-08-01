package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.content.ScrapingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Firstworks.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Firstworks.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BarrelRecipe>> BARREL_PROCESSING_TYPE = TYPES.register(
            "barrel_processing", () -> RecipeType.simple(Firstworks.id("barrel_processing")));
    public static final DeferredHolder<RecipeSerializer<?>, BarrelRecipe.Serializer> BARREL_PROCESSING_SERIALIZER = SERIALIZERS.register(
            "barrel_processing", BarrelRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, ScrapingRecipe.Serializer> SCRAPING_SERIALIZER = SERIALIZERS.register(
            "scraping", ScrapingRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private ModRecipes() {}
}
