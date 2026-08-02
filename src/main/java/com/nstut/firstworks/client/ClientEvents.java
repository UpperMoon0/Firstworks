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
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 1 ? 0xFF6B3F22 : 0xFFFFFFFF,
                ModItems.TANNIN_SOLUTION_BUCKET.get());
        event.register((stack, tintIndex) -> tintIndex == 0
                        ? ColoredFleeceItem.color(stack).getTextureDiffuseColor()
                        : 0xFFFFFFFF,
                ModItems.RAW_FLEECE.get(), ModItems.CLEAN_WOOL.get());
    }

    private ClientEvents() {}
}
