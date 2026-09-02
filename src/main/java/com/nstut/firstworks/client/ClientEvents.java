package com.nstut.firstworks.client;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModFluids;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

@EventBusSubscriber(modid = Firstworks.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(Firstworks.id("block/quern_runner")));
        event.register(ModelResourceLocation.standalone(Firstworks.id("block/rotary_quern_runner")));
        event.register(WorkshopBlockEntityRenderer.POTTERY_HEAD);
        event.register(WorkshopBlockEntityRenderer.KILN_EMBERS);
        event.register(WorkshopBlockEntityRenderer.CRUCIBLE_CONTENTS);
        event.register(BellowsBlockEntityRenderer.BAG);
        event.register(BellowsBlockEntityRenderer.TOP);
        event.register(LoomBlockEntityRenderer.COPPER_BEATER_MODEL);
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(TreeBarkTextureManager.INSTANCE);
    }

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
        event.registerBlockEntityRenderer(ModBlockEntities.MORTAR.get(), MortarBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.QUERN.get(), QuernBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WORKSHOP.get(), WorkshopBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BELLOWS.get(), BellowsBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0
                        ? ColoredFleeceItem.color(stack).getTextureDiffuseColor()
                        : 0xFFFFFFFF,
                ModItems.RAW_FLEECE.get(), ModItems.CLEAN_WOOL.get());

        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) return 0xFFFFFFFF;
            return IClientFluidTypeExtensions.of(Fluids.WATER).getTintColor();
        }, ModItems.WATER_CLAY_BUCKET.get());

        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) return 0xFFFFFFFF;
            return IClientFluidTypeExtensions.of(ModFluids.TANNIN_SOLUTION.get()).getTintColor();
        }, ModItems.TANNIN_CLAY_BUCKET.get(), ModItems.TANNIN_SOLUTION_BUCKET.get());
    }

    private ClientEvents() {}
}
