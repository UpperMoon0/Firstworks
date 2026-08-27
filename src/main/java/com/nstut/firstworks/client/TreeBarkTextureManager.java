package com.nstut.firstworks.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.nstut.firstworks.Firstworks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStream;
import java.util.*;

public final class TreeBarkTextureManager implements ResourceManagerReloadListener {

    public static final TreeBarkTextureManager INSTANCE = new TreeBarkTextureManager();

    private static final ResourceLocation MASK_LOC = Firstworks.id("textures/item/tree_bark_mask.png");
    private static final ResourceLocation SHADE_LOC = Firstworks.id("textures/item/tree_bark_shade.png");

    private final Map<String, ResourceLocation> TEXTURE_LOCATIONS = new HashMap<>();
    private final Map<String, List<QuadVertex>> MESH_CACHE = new HashMap<>();

    public record QuadVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}

    private TreeBarkTextureManager() {}

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        TEXTURE_LOCATIONS.clear();
        MESH_CACHE.clear();

        NativeImage mask = loadNativeImage(resourceManager, MASK_LOC);
        NativeImage shade = loadNativeImage(resourceManager, SHADE_LOC);

        if (mask == null || shade == null) {
            System.err.println("[Firstworks] Failed to load tree bark mask or shade texture!");
            if (mask != null) mask.close();
            if (shade != null) shade.close();
            return;
        }

        Set<String> woodTypes = discoverWoodTypes();

        for (String woodType : woodTypes) {
            ResourceLocation logTexLoc = getLogTextureLocation(woodType);
            NativeImage logTex = loadNativeImage(resourceManager, logTexLoc);
            if (logTex == null && !"oak".equals(woodType)) {
                continue;
            }
            if (logTex == null) {
                logTex = new NativeImage(16, 16, true);
                logTex.fillRect(0, 0, 16, 16, 0xFF997345);
            }

            NativeImage barkImage = processBark(logTex, mask, shade);
            logTex.close();

            ResourceLocation dynLoc = Firstworks.id("dynamic_bark/" + woodType.replace(':', '/'));
            Minecraft.getInstance().getTextureManager().register(dynLoc, new DynamicTexture(barkImage));
            TEXTURE_LOCATIONS.put(woodType, dynLoc);

            List<QuadVertex> mesh = buildMesh(woodType, barkImage);
            MESH_CACHE.put(woodType, mesh);
        }

        mask.close();
        shade.close();
        Firstworks.LOGGER.info("Dynamically generated tree bark textures for {} wood types.", TEXTURE_LOCATIONS.size());
    }

    public static ResourceLocation getTextureLocation(String woodType) {
        return INSTANCE.TEXTURE_LOCATIONS.getOrDefault(woodType, INSTANCE.TEXTURE_LOCATIONS.getOrDefault("oak", Firstworks.id("dynamic_bark/oak")));
    }

    public static List<QuadVertex> getMesh(String woodType) {
        return INSTANCE.MESH_CACHE.getOrDefault(woodType, INSTANCE.MESH_CACHE.getOrDefault("oak", Collections.emptyList()));
    }

    private static Set<String> discoverWoodTypes() {
        Set<String> set = new LinkedHashSet<>();
        set.addAll(com.nstut.firstworks.registry.WoodTypeRegistry.explicitWoodTypes());
        set.addAll(Arrays.asList(
                "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "mangrove", "cherry", "bamboo", "crimson", "warped"
        ));

        for (ResourceLocation loc : BuiltInRegistries.BLOCK.keySet()) {
            String path = loc.getPath();
            if (path.endsWith("_log") || path.endsWith("_stem")) {
                String woodType = loc.getNamespace().equals("minecraft")
                        ? path.substring(0, path.lastIndexOf('_'))
                        : loc.getNamespace() + ":" + path.substring(0, path.lastIndexOf('_'));
                set.add(woodType);
            }
        }
        return set;
    }

    private static ResourceLocation getLogTextureLocation(String woodType) {
        ResourceLocation explicit = com.nstut.firstworks.registry.WoodTypeRegistry.getLogTexture(woodType);
        if (explicit != null) {
            return explicit.withPath(path -> path.startsWith("textures/") ? path : "textures/" + path);
        }
        if ("bamboo".equals(woodType)) {
            return ResourceLocation.withDefaultNamespace("textures/block/bamboo_block.png");
        } else if ("crimson".equals(woodType)) {
            return ResourceLocation.withDefaultNamespace("textures/block/crimson_stem.png");
        } else if ("warped".equals(woodType)) {
            return ResourceLocation.withDefaultNamespace("textures/block/warped_stem.png");
        } else if (woodType.contains(":")) {
            String[] parts = woodType.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], "textures/block/" + parts[1] + "_log.png");
        }
        return ResourceLocation.withDefaultNamespace("textures/block/" + woodType + "_log.png");
    }

    private static NativeImage loadNativeImage(ResourceManager rm, ResourceLocation loc) {
        try {
            var resource = rm.getResource(loc);
            if (resource.isPresent()) {
                try (InputStream is = resource.get().open()) {
                    return NativeImage.read(is);
                }
            }
        } catch (Exception e) {
            // Ignore missing texture
        }
        return null;
    }

    private static NativeImage processBark(NativeImage logTex, NativeImage mask, NativeImage shade) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        NativeImage result = new NativeImage(width, height, true);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int maskRgb = mask.getPixelRGBA(x, y);
                int maskAlpha = (maskRgb >>> 24) & 0xFF;

                if (maskAlpha < 10) {
                    result.setPixelRGBA(x, y, 0);
                    continue;
                }

                int logRgb = logTex.getPixelRGBA(x % logTex.getWidth(), y % logTex.getHeight());
                int logA = (logRgb >>> 24) & 0xFF;
                int logR = logRgb & 0xFF;
                int logG = (logRgb >> 8) & 0xFF;
                int logB = (logRgb >> 16) & 0xFF;

                int shadeRgb = shade.getPixelRGBA(x, y);
                int shadeA = (shadeRgb >>> 24) & 0xFF;
                int shadeR = shadeRgb & 0xFF;
                int shadeG = (shadeRgb >> 8) & 0xFF;
                int shadeB = (shadeRgb >> 16) & 0xFF;

                int finalA = (logA * maskAlpha) / 255;
                int finalR = logR;
                int finalG = logG;
                int finalB = logB;

                if (shadeA > 0) {
                    int shadedR = (logR * shadeR) / 255;
                    int shadedG = (logG * shadeG) / 255;
                    int shadedB = (logB * shadeB) / 255;

                    float factor = (shadeA / 255.0f) * 0.45f;
                    finalR = (int) (logR * (1.0f - factor) + shadedR * factor);
                    finalG = (int) (logG * (1.0f - factor) + shadedG * factor);
                    finalB = (int) (logB * (1.0f - factor) + shadedB * factor);
                }

                int finalPixel = (finalA << 24) | (finalB << 16) | (finalG << 8) | finalR;
                result.setPixelRGBA(x, y, finalPixel);
            }
        }

        return result;
    }

    private static List<QuadVertex> buildMesh(String woodType, NativeImage img) {
        List<QuadVertex> vertices = new ArrayList<>();
        int w = img.getWidth();
        int h = img.getHeight();
        float pw = 1.0f / w;
        float ph = 1.0f / h;

        float cx = 0.5f;
        float cy = 0.5f;

        float zFront = 0.03125f;
        float zBack = -0.03125f;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (img.getPixelRGBA(x, y) >>> 24) & 0xFF;
                if (alpha < 10) continue;

                float x1 = x * pw - cx;
                float x2 = (x + 1) * pw - cx;
                float y1 = 0.5f - y * ph;
                float y2 = 0.5f - (y + 1) * ph;

                float u1 = x * pw;
                float u2 = (x + 1) * pw;
                float v1 = y * ph;
                float v2 = (y + 1) * ph;

                // Front Face (Z = zFront, Normal: 0, 0, 1)
                addQuad(vertices,
                        x1, y2, zFront, u1, v2,
                        x1, y1, zFront, u1, v1,
                        x2, y1, zFront, u2, v1,
                        x2, y2, zFront, u2, v2,
                        0, 0, 1);

                // Back Face (Z = zBack, Normal: 0, 0, -1)
                addQuad(vertices,
                        x2, y2, zBack, u2, v2,
                        x2, y1, zBack, u2, v1,
                        x1, y1, zBack, u1, v1,
                        x1, y2, zBack, u1, v2,
                        0, 0, -1);

                float edgeU = (x + 0.5f) * pw;
                float edgeV = (y + 0.5f) * ph;

                // Top edge
                if (y == 0 || ((img.getPixelRGBA(x, y - 1) >>> 24) & 0xFF) < 10) {
                    addQuad(vertices,
                            x1, y1, zBack, edgeU, edgeV,
                            x1, y1, zFront, edgeU, edgeV,
                            x2, y1, zFront, edgeU, edgeV,
                            x2, y1, zBack, edgeU, edgeV,
                            0, 1, 0);
                }

                // Bottom edge
                if (y == h - 1 || ((img.getPixelRGBA(x, y + 1) >>> 24) & 0xFF) < 10) {
                    addQuad(vertices,
                            x2, y2, zBack, edgeU, edgeV,
                            x2, y2, zFront, edgeU, edgeV,
                            x1, y2, zFront, edgeU, edgeV,
                            x1, y2, zBack, edgeU, edgeV,
                            0, -1, 0);
                }

                // Left edge
                if (x == 0 || ((img.getPixelRGBA(x - 1, y) >>> 24) & 0xFF) < 10) {
                    addQuad(vertices,
                            x1, y2, zBack, edgeU, edgeV,
                            x1, y2, zFront, edgeU, edgeV,
                            x1, y1, zFront, edgeU, edgeV,
                            x1, y1, zBack, edgeU, edgeV,
                            -1, 0, 0);
                }

                // Right edge
                if (x == w - 1 || ((img.getPixelRGBA(x + 1, y) >>> 24) & 0xFF) < 10) {
                    addQuad(vertices,
                            x2, y1, zBack, edgeU, edgeV,
                            x2, y1, zFront, edgeU, edgeV,
                            x2, y2, zFront, edgeU, edgeV,
                            x2, y2, zBack, edgeU, edgeV,
                            1, 0, 0);
                }
            }
        }

        return vertices;
    }

    private static void addQuad(List<QuadVertex> list,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                float x4, float y4, float z4, float u4, float v4,
                                float nx, float ny, float nz) {
        list.add(new QuadVertex(x1, y1, z1, u1, v1, nx, ny, nz));
        list.add(new QuadVertex(x2, y2, z2, u2, v2, nx, ny, nz));
        list.add(new QuadVertex(x3, y3, z3, u3, v3, nx, ny, nz));
        list.add(new QuadVertex(x4, y4, z4, u4, v4, nx, ny, nz));
    }
}
