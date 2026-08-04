package com.nstut.firstworks.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.content.TreeBarkItem;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Firstworks.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private static final int DEFAULT_BARK_COLOR = 0xFF997345;
    private static final Map<String, Integer> WOOD_COLOR_CACHE = new HashMap<>();

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
            ItemProperties.register(ModItems.HAND_SPINDLE.get(), Firstworks.id("spinning"),
                    (stack, level, entity, seed) -> {
                        if (entity == null || !entity.isUsingItem()
                                || !entity.getUseItem().is(ModItems.HAND_SPINDLE.get())) return 0.0F;
                        return switch ((entity.getTicksUsingItem() / 2) % 4) {
                            case 1 -> 0.34F;
                            case 2 -> 0.67F;
                            case 3 -> 1.0F;
                            default -> 0.0F;
                        };
                    })
        );
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BARREL.get(), BarrelBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LOOM.get(), LoomBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BRICK_MOLD.get(), BrickMoldBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0
                        ? ColoredFleeceItem.color(stack).getTextureDiffuseColor()
                        : 0xFFFFFFFF,
                ModItems.RAW_FLEECE.get(), ModItems.CLEAN_WOOL.get());
        event.register((stack, tintIndex) -> tintIndex == 0
                        ? getWoodTypeColor(TreeBarkItem.woodType(stack))
                        : 0xFFFFFFFF,
                ModItems.TREE_BARK.get());
    }

    private static int getWoodTypeColor(String woodType) {
        if (woodType == null || woodType.isEmpty()) return DEFAULT_BARK_COLOR;
        return WOOD_COLOR_CACHE.computeIfAbsent(woodType, ClientEvents::sampleWoodTextureColor);
    }

    private static int sampleWoodTextureColor(String woodType) {
        try {
            String namespace = "minecraft";
            String textureName = woodType + "_log";
            if (woodType.contains(":")) {
                String[] parts = woodType.split(":", 2);
                namespace = parts[0];
                textureName = parts[1] + "_log";
            } else if ("bamboo".equals(woodType)) {
                textureName = "bamboo_block";
            } else if ("crimson".equals(woodType)) {
                textureName = "crimson_stem";
            } else if ("warped".equals(woodType)) {
                textureName = "warped_stem";
            }

            ResourceLocation loc = ResourceLocation.tryBuild(namespace, "textures/block/" + textureName + ".png");
            if (loc == null) return DEFAULT_BARK_COLOR;

            var resource = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (resource.isEmpty()) return DEFAULT_BARK_COLOR;

            try (var is = resource.get().open();
                 var image = NativeImage.read(is)) {
                int width = image.getWidth();
                int height = image.getHeight();
                long rSum = 0, gSum = 0, bSum = 0, count = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        int a = (pixel >>> 24) & 0xFF;
                        if (a > 10) {
                            rSum += pixel & 0xFF;
                            gSum += (pixel >> 8) & 0xFF;
                            bSum += (pixel >> 16) & 0xFF;
                            count++;
                        }
                    }
                }
                if (count > 0) {
                    int avgR = (int) (rSum / count);
                    int avgG = (int) (gSum / count);
                    int avgB = (int) (bSum / count);
                    return 0xFF000000 | (avgR << 16) | (avgG << 8) | avgB;
                }
            }
        } catch (Exception ignored) {}

        return DEFAULT_BARK_COLOR;
    }

    private ClientEvents() {}
}
