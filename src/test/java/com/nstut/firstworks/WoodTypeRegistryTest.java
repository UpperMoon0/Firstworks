package com.nstut.firstworks;

import com.nstut.firstworks.registry.WoodTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WoodTypeRegistryTest {

    @Test
    void testConventionalVanillaLogs() {
        assertEquals("oak", WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("minecraft", "oak_log")));
        assertEquals("crimson", WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("minecraft", "crimson_stem")));
        assertEquals("warped", WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("minecraft", "warped_hyphae")));
        assertEquals("birch", WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("minecraft", "birch_wood")));
        assertEquals("bamboo", WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("minecraft", "bamboo_block")));
    }

    @Test
    void testConventionalModdedLogs() {
        assertEquals("regions_unexplored:baobab", WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("regions_unexplored", "baobab_log")));
        assertEquals("testmod:magic", WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("testmod", "magic_wood")));
        assertEquals("testmod:fungus", WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("testmod", "fungus_stem")));
        assertEquals("testmod:nether", WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("testmod", "nether_hyphae")));
    }

    @Test
    void testNonWoodBlocksReturnNull() {
        assertNull(WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("minecraft", "stone")));
        assertNull(WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("mod", "painted_pillar")));
        assertNull(WoodTypeRegistry.tryResolveWoodType(ResourceLocation.fromNamespaceAndPath("minecraft", "dirt")));
    }

    @Test
    void testExplicitRegistration() {
        ResourceLocation customLog = ResourceLocation.fromNamespaceAndPath("custommod", "ancient_tree");
        ResourceLocation customTexture = ResourceLocation.fromNamespaceAndPath("custommod", "block/ancient_tree");
        WoodTypeRegistry.register(customLog, "custommod:ancient", customTexture, "Ancient");

        assertEquals("custommod:ancient", WoodTypeRegistry.tryResolveWoodType(customLog));
        assertEquals("Ancient", WoodTypeRegistry.getDisplayName("custommod:ancient"));
        assertEquals(customTexture, WoodTypeRegistry.getLogTexture("custommod:ancient"));
    }

    @Test
    void testVisualFallbackResolveWoodType() {
        assertEquals("oak", WoodTypeRegistry.resolveWoodType(ResourceLocation.fromNamespaceAndPath("minecraft", "stone")));
        assertEquals("spruce", WoodTypeRegistry.resolveWoodType(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_log")));
    }
}
