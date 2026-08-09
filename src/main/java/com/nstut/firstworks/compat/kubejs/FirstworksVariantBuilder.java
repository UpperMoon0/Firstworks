package com.nstut.firstworks.compat.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;

abstract class FirstworksVariantBuilder extends BlockBuilder {
    protected ResourceLocation planks;
    protected ResourceLocation slab;
    protected ResourceLocation plankTexture;
    protected boolean generateRecipe;

    protected FirstworksVariantBuilder(ResourceLocation id) {
        super(id);
        tagBlock(new ResourceLocation[]{BlockTags.MINEABLE_WITH_AXE.location()});
    }

    protected void configureCommon(ResourceLocation planks, ResourceLocation slab, ResourceLocation plankTexture,
            String displayName, String kind, boolean generateRecipe) {
        this.planks = planks;
        this.slab = slab;
        this.plankTexture = plankTexture;
        this.generateRecipe = generateRecipe;
        displayName(Component.literal(displayName + " " + kind));
    }

    @Override
    public void generateData(KubeDataGenerator generator) {
        super.generateData(generator);
        if (generateRecipe) {
            generator.json(resource("recipe/" + id.getPath()), createRecipe());
        }
    }

    protected abstract JsonObject createRecipe();

    protected JsonObject shapedRecipe(String[] pattern) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "misc");

        JsonArray patternJson = new JsonArray();
        for (String row : pattern) patternJson.add(row);
        recipe.add("pattern", patternJson);

        JsonObject key = new JsonObject();
        key.add("P", ingredient(planks));
        key.add("S", ingredient(slab));
        recipe.add("key", key);

        JsonObject result = new JsonObject();
        result.addProperty("id", id.toString());
        recipe.add("result", result);
        return recipe;
    }

    protected static JsonObject ingredient(ResourceLocation item) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", item.toString());
        return ingredient;
    }

    protected ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
    }

    protected static JsonObject model(ResourceLocation parent, MapEntry... textures) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent.toString());
        JsonObject textureJson = new JsonObject();
        for (MapEntry texture : textures) textureJson.addProperty(texture.key(), texture.value().toString());
        model.add("textures", textureJson);
        return model;
    }

    protected record MapEntry(String key, ResourceLocation value) {}
}
