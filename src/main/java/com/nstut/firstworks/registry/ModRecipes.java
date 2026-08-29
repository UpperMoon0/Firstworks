package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelRecipe;
import com.nstut.firstworks.content.brick_mold.BrickMoldingRecipe;
import com.nstut.firstworks.content.loom.LoomRecipe;
import com.nstut.firstworks.content.ScrapingRecipe;
import com.nstut.firstworks.content.SpinningRecipe;
import com.nstut.firstworks.content.MortarGrindingRecipe;
import com.nstut.firstworks.content.FleeceDyeingRecipe;
import com.nstut.firstworks.content.WoolBlockRecipe;
import com.nstut.firstworks.content.TextileBedRecipe;
import com.nstut.firstworks.content.quern.QuernGrindingRecipe;
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
    public static final DeferredHolder<RecipeType<?>, RecipeType<LoomRecipe>> LOOM_WEAVING_TYPE = TYPES.register(
            "loom_weaving", () -> RecipeType.simple(Firstworks.id("loom_weaving")));
    public static final DeferredHolder<RecipeSerializer<?>, LoomRecipe.Serializer> LOOM_WEAVING_SERIALIZER = SERIALIZERS.register(
            "loom_weaving", LoomRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<BrickMoldingRecipe>> BRICK_MOLDING_TYPE = TYPES.register(
            "brick_molding", () -> RecipeType.simple(Firstworks.id("brick_molding")));
    public static final DeferredHolder<RecipeSerializer<?>, BrickMoldingRecipe.Serializer> BRICK_MOLDING_SERIALIZER = SERIALIZERS.register(
            "brick_molding", BrickMoldingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, ScrapingRecipe.Serializer> SCRAPING_SERIALIZER = SERIALIZERS.register(
            "scraping", ScrapingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<SpinningRecipe>> SPINNING_TYPE = TYPES.register(
            "spinning", () -> RecipeType.simple(Firstworks.id("spinning")));
    public static final DeferredHolder<RecipeSerializer<?>, SpinningRecipe.Serializer> SPINNING_SERIALIZER = SERIALIZERS.register(
            "spinning", SpinningRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MortarGrindingRecipe>> MORTAR_GRINDING_TYPE = TYPES.register(
            "mortar_grinding", () -> RecipeType.simple(Firstworks.id("mortar_grinding")));
    public static final DeferredHolder<RecipeSerializer<?>, MortarGrindingRecipe.Serializer> MORTAR_GRINDING_SERIALIZER = SERIALIZERS.register(
            "mortar_grinding", MortarGrindingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<QuernGrindingRecipe>> QUERN_GRINDING_TYPE = TYPES.register(
            "quern_grinding", () -> RecipeType.simple(Firstworks.id("quern_grinding")));
    public static final DeferredHolder<RecipeSerializer<?>, QuernGrindingRecipe.Serializer> QUERN_GRINDING_SERIALIZER = SERIALIZERS.register(
            "quern_grinding", QuernGrindingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, FleeceDyeingRecipe.Serializer> FLEECE_DYEING_SERIALIZER = SERIALIZERS.register(
            "fleece_dyeing", FleeceDyeingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, WoolBlockRecipe.Serializer> WOOL_BLOCK_SERIALIZER = SERIALIZERS.register(
            "wool_block", WoolBlockRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, TextileBedRecipe.Serializer> TEXTILE_BED_SERIALIZER = SERIALIZERS.register(
            "textile_bed", TextileBedRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private ModRecipes() {}
}
