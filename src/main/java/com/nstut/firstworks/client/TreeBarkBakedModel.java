package com.nstut.firstworks.client;

import com.nstut.firstworks.content.TreeBarkItem;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

import java.util.List;
import java.util.Map;

public final class TreeBarkBakedModel extends BakedModelWrapper<BakedModel> {

    private final Map<String, BakedModel> variants;

    public TreeBarkBakedModel(BakedModel original, Map<String, BakedModel> variants) {
        super(original);
        this.variants = variants;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean transform) {
        String woodType = TreeBarkItem.woodType(stack);
        BakedModel variant = variants.get(woodType);
        if (variant == null) {
            variant = variants.get("oak");
        }
        return List.of(variant != null ? variant : originalModel);
    }
}
