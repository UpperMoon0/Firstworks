package com.nstut.firstworks.compat.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelBlock;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class FirstworksBarrelBuilder extends FirstworksVariantBuilder {
    private ResourceLocation logTexture;
    private ResourceLocation logTopTexture;

    public FirstworksBarrelBuilder(ResourceLocation id) {
        super(id);
        tagBlock(new ResourceLocation[]{Firstworks.id("barrels")});
        tagItem(new ResourceLocation[]{Firstworks.id("barrels")});
    }

    void configure(ResourceLocation planks, ResourceLocation slab, ResourceLocation plankTexture,
            ResourceLocation logTexture, ResourceLocation logTopTexture, String displayName, boolean generateRecipe) {
        configureCommon(planks, slab, plankTexture, displayName, "Barrel", generateRecipe);
        this.logTexture = logTexture;
        this.logTopTexture = logTopTexture;
    }

    @Override
    public Block createObject() {
        return new BarrelBlock(Block.Properties.ofFullCopy(Blocks.BARREL).noOcclusion());
    }

    @Override
    public void generateAssets(KubeAssetGenerator generator) {
        ResourceLocation openModel = resource("block/" + id.getPath() + "_open");
        ResourceLocation sealedModel = resource("block/" + id.getPath() + "_sealed");

        JsonArray multipart = new JsonArray();
        multipart.add(part("false", openModel));
        multipart.add(part("true", sealedModel));
        JsonObject blockState = new JsonObject();
        blockState.add("multipart", multipart);
        generator.json(resource("blockstates/" + id.getPath()), blockState);

        generator.json(resource("models/block/" + id.getPath() + "_open"),
                model(Firstworks.id("block/barrel_open_template"), new MapEntry("wood", plankTexture)));
        generator.json(resource("models/block/" + id.getPath() + "_sealed"),
                model(Firstworks.id("block/barrel_sealed_template"), new MapEntry("wood", plankTexture),
                        new MapEntry("log", logTexture), new MapEntry("log_top", logTopTexture)));

        JsonObject itemModel = new JsonObject();
        itemModel.addProperty("parent", openModel.toString());
        generator.json(resource("models/item/" + id.getPath()), itemModel);
    }

    @Override
    protected JsonObject createRecipe() {
        return shapedRecipe(new String[]{"SPS", "P P", "SPS"});
    }

    private static JsonObject part(String sealed, ResourceLocation model) {
        JsonObject when = new JsonObject();
        when.addProperty("sealed", sealed);
        JsonObject apply = new JsonObject();
        apply.addProperty("model", model.toString());
        JsonObject part = new JsonObject();
        part.add("when", when);
        part.add("apply", apply);
        return part;
    }
}
