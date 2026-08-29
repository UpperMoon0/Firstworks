package com.nstut.firstworks;

import com.mojang.logging.LogUtils;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModCreativeTabs;
import com.nstut.firstworks.registry.ModDataComponents;
import com.nstut.firstworks.registry.ModFluids;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

@Mod(Firstworks.MOD_ID)
public final class Firstworks {
    public static final String MOD_ID = "firstworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Firstworks(IEventBus modBus, ModContainer container) {
        ModDataComponents.register(modBus);
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        ModBlocks.register(modBus);
        ModFluids.register(modBus);
        ModRecipes.register(modBus);
        ModBlockEntities.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, FirstworksConfig.SPEC);
        LOGGER.info("Firstworks is preparing the first works");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
