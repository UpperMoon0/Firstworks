package com.nstut.firstworks.client;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.client.LoomBlockEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.item.ItemProperties;

@EventBusSubscriber(modid = Firstworks.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.HAND_SPINDLE.get(), Firstworks.id("spinning"),
                (stack, level, entity, seed) -> {
                    if (entity == null || !entity.isUsingItem()
                            || !entity.getUseItem().is(ModItems.HAND_SPINDLE.get())) return 0.0F;
                    return switch ((entity.getTicksUsingItem() / 2) % 4) {
                        case 1 -> 0.34F;
                        case 2 -> 0.67F;
                        case 3 -> 1.0F;
                        default -> 0.0F;
                    };
                }));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BARREL.get(), BarrelBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LOOM.get(), LoomBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BRICK_MOLD.get(), BrickMoldBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 1 ? 0xFF6B3F22 : 0xFFFFFFFF,
                ModItems.TANNIN_SOLUTION_BUCKET.get());
        event.register((stack, tintIndex) -> tintIndex == 1 ? 0xFF3F76E4 : 0xFFFFFFFF,
                ModItems.WATER_CLAY_BUCKET.get());
        event.register((stack, tintIndex) -> tintIndex == 1 ? 0xFF6B3F22 : 0xFFFFFFFF,
                ModItems.TANNIN_CLAY_BUCKET.get());
        event.register((stack, tintIndex) -> tintIndex == 0
                        ? ColoredFleeceItem.color(stack).getTextureDiffuseColor()
                        : 0xFFFFFFFF,
                ModItems.RAW_FLEECE.get(), ModItems.CLEAN_WOOL.get());
        event.register((stack, tintIndex) -> tintIndex == 0
                        ? getWoodTypeColor(com.nstut.firstworks.content.TreeBarkItem.woodType(stack))
                        : 0xFFFFFFFF,
                ModItems.TREE_BARK.get());
    }

    private static int getWoodTypeColor(String woodType) {
        return switch (woodType) {
            case "spruce" -> 0xFF604329;
            case "birch" -> 0xFFD7CBB4;
            case "jungle" -> 0xFF56431A;
            case "acacia" -> 0xFF6D6456;
            case "dark_oak" -> 0xFF392815;
            case "mangrove" -> 0xFF4F2A1E;
            case "cherry" -> 0xFFD48E8D;
            case "bamboo" -> 0xFF647434;
            case "crimson" -> 0xFF6B293C;
            case "warped" -> 0xFF3A8E89;
            default -> 0xFF997345; // oak
        };
    }

    private ClientEvents() {}
}
