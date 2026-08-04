package com.nstut.firstworks;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TreeBarkTextureGenerator {

    public static void main(String[] args) throws Exception {
        File maskFile = new File("src/main/resources/assets/firstworks/textures/item/tree_bark_mask.png");
        File shadeFile = new File("src/main/resources/assets/firstworks/textures/item/tree_bark_shade.png");

        if (!maskFile.exists() || !shadeFile.exists()) {
            System.err.println("Mask or shade file missing!");
            return;
        }

        BufferedImage mask = ImageIO.read(maskFile);
        BufferedImage shade = ImageIO.read(shadeFile);

        System.out.println("Mask size: " + mask.getWidth() + "x" + mask.getHeight());
        System.out.println("Shade size: " + shade.getWidth() + "x" + shade.getHeight());

        File clientJar = new File("C:/Users/NsTut/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar");
        if (!clientJar.exists()) {
            System.err.println("Client jar not found: " + clientJar.getAbsolutePath());
            return;
        }

        Map<String, String> woodToTexture = new HashMap<>();
        woodToTexture.put("tree_bark", "assets/minecraft/textures/block/oak_log.png");
        woodToTexture.put("oak_tree_bark", "assets/minecraft/textures/block/oak_log.png");
        woodToTexture.put("spruce_tree_bark", "assets/minecraft/textures/block/spruce_log.png");
        woodToTexture.put("birch_tree_bark", "assets/minecraft/textures/block/birch_log.png");
        woodToTexture.put("jungle_tree_bark", "assets/minecraft/textures/block/jungle_log.png");
        woodToTexture.put("acacia_tree_bark", "assets/minecraft/textures/block/acacia_log.png");
        woodToTexture.put("dark_oak_tree_bark", "assets/minecraft/textures/block/dark_oak_log.png");
        woodToTexture.put("mangrove_tree_bark", "assets/minecraft/textures/block/mangrove_log.png");
        woodToTexture.put("cherry_tree_bark", "assets/minecraft/textures/block/cherry_log.png");
        woodToTexture.put("bamboo_tree_bark", "assets/minecraft/textures/block/bamboo_block.png");
        woodToTexture.put("crimson_tree_bark", "assets/minecraft/textures/block/crimson_stem.png");
        woodToTexture.put("warped_tree_bark", "assets/minecraft/textures/block/warped_stem.png");

        ZipFile zip = new ZipFile(clientJar);

        File outputDir = new File("src/main/resources/assets/firstworks/textures/item");
        File modelDir = new File("src/main/resources/assets/firstworks/models/item");
        modelDir.mkdirs();

        for (Map.Entry<String, String> entry : woodToTexture.entrySet()) {
            String barkName = entry.getKey();
            String texPath = entry.getValue();

            ZipEntry zipEntry = zip.getEntry(texPath);
            if (zipEntry == null) {
                System.out.println("Log texture entry missing for " + barkName + ": " + texPath);
                continue;
            }

            BufferedImage logTex;
            try (InputStream is = zip.getInputStream(zipEntry)) {
                logTex = ImageIO.read(is);
            }

            BufferedImage result = processBark(logTex, mask, shade);

            File outFile = new File(outputDir, barkName + ".png");
            ImageIO.write(result, "PNG", outFile);
            System.out.println("Generated texture: " + outFile.getPath());

            // Generate item model json
            String modelJson = "{\n" +
                    "  \"parent\": \"minecraft:item/generated\",\n" +
                    "  \"textures\": {\n" +
                    "    \"layer0\": \"firstworks:item/" + barkName + "\"\n" +
                    "  }\n" +
                    "}\n";

            File modelFile = new File(modelDir, barkName + ".json");
            java.nio.file.Files.writeString(modelFile.toPath(), modelJson);
            System.out.println("Generated model: " + modelFile.getPath());
        }

        zip.close();
        System.out.println("DONE generating tree bark textures and models!");
    }

    private static BufferedImage processBark(BufferedImage logTex, BufferedImage mask, BufferedImage shade) {
        int width = mask.getWidth();
        int height = mask.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int maskRgb = mask.getRGB(x, y);
                int maskAlpha = (maskRgb >> 24) & 0xFF;

                if (maskAlpha == 0) {
                    result.setRGB(x, y, 0); // fully transparent
                    continue;
                }

                int logRgb = logTex.getRGB(x % logTex.getWidth(), y % logTex.getHeight());
                int logA = (logRgb >> 24) & 0xFF;
                int logR = (logRgb >> 16) & 0xFF;
                int logG = (logRgb >> 8) & 0xFF;
                int logB = logRgb & 0xFF;

                int shadeRgb = shade.getRGB(x, y);
                int shadeA = (shadeRgb >> 24) & 0xFF;
                int shadeR = (shadeRgb >> 16) & 0xFF;
                int shadeG = (shadeRgb >> 8) & 0xFF;
                int shadeB = shadeRgb & 0xFF;

                // Combine mask alpha
                int finalA = (logA * maskAlpha) / 255;

                // Multiply / Overlay shading layer onto log color
                int finalR = (logR * shadeR) / 255;
                int finalG = (logG * shadeG) / 255;
                int finalB = (logB * shadeB) / 255;

                // Blend shading alpha if shade has transparent regions
                if (shadeA < 255) {
                    float factor = shadeA / 255.0f;
                    finalR = (int) (logR * (1 - factor) + finalR * factor);
                    finalG = (int) (logG * (1 - factor) + finalG * factor);
                    finalB = (int) (logB * (1 - factor) + finalB * factor);
                }

                int finalPixel = (finalA << 24) | (finalR << 16) | (finalG << 8) | finalB;
                result.setRGB(x, y, finalPixel);
            }
        }

        return result;
    }
}
