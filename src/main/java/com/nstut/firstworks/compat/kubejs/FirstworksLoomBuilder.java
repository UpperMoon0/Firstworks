package com.nstut.firstworks.compat.kubejs;

import com.google.gson.JsonObject;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.loom.LoomBlock;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class FirstworksLoomBuilder extends FirstworksVariantBuilder {
    private ResourceLocation logTexture;

    public FirstworksLoomBuilder(ResourceLocation id) {
        super(id);
    }

    void configure(ResourceLocation planks, ResourceLocation slab, ResourceLocation plankTexture,
            ResourceLocation logTexture, String displayName, boolean generateRecipe) {
        configureCommon(planks, slab, plankTexture, displayName, "Loom", generateRecipe);
        this.logTexture = logTexture;
    }

    @Override
    public Block createObject() {
        return new LoomBlock(Block.Properties.ofFullCopy(Blocks.LOOM).noOcclusion());
    }

    @Override
    public void generateAssets(KubeAssetGenerator generator) {
        ResourceLocation blockModel = resource("block/" + id.getPath());

        JsonObject variants = new JsonObject();
        variants.add("facing=north", variant(blockModel, 0));
        variants.add("facing=east", variant(blockModel, 90));
        variants.add("facing=south", variant(blockModel, 180));
        variants.add("facing=west", variant(blockModel, 270));
        JsonObject blockState = new JsonObject();
        blockState.add("variants", variants);
        generator.json(resource("blockstates/" + id.getPath()), blockState);

        generator.json(resource("models/block/" + id.getPath()), model(Firstworks.id("block/loom_frame_template"),
                new MapEntry("wood", plankTexture), new MapEntry("log", logTexture)));

        JsonObject itemModel = new JsonObject();
        itemModel.addProperty("parent", blockModel.toString());
        generator.json(resource("models/item/" + id.getPath()), itemModel);
    }

    @Override
    protected JsonObject createRecipe() {
        return shapedRecipe(new String[]{"P P", "SPS", "P P"});
    }

    private static JsonObject variant(ResourceLocation model, int rotation) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", model.toString());
        if (rotation != 0) variant.addProperty("y", rotation);
        return variant;
    }
}
