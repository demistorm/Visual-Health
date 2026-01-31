package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;

import java.util.Random;

// Generates composite textures with wound effects stamped onto them
// Creates entity-sized textures with wound overlays for the RenderLayer system
public class WoundTextureGenerator {

    private WoundTextureGenerator() {
        // Utility class - no instances
    }

    // Generate a texture with wounds stamped onto it
    // Returns a texture identifier that can be used with entity models
    public static Identifier generateWoundedTexture(LivingEntity entity, int damageTier, int baseWidth, int baseHeight) {
        try {
            // Create a new NativeImage with RGBA format (supports transparency)
            NativeImage woundTexture = new NativeImage(baseWidth, baseHeight, true);

            // Fill with transparent black (fully transparent) - ARGB format
            int transparent = 0x00000000; // A=0, R=0, G=0, B=0
            for (int y = 0; y < baseHeight; y++) {
                for (int x = 0; x < baseWidth; x++) {
                    woundTexture.setPixel(x, y, transparent);
                }
            }

            // Calculate number of wounds based on damage tier
            // Tier 1: 2 wounds, Tier 2: 4 wounds, Tier 3: 6 wounds, Tier 4: 8 wounds
            int woundCount = damageTier * 2;

            // Use entity UUID for consistent random seed
            Random random = new Random(entity.getUUID().getLeastSignificantBits());

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Generating {}x{} wound texture with {} wounds for {}",
                        baseWidth, baseHeight, woundCount, entity.getName().getString());
            }

            // Stamp wound textures onto the base texture
            for (int i = 0; i < woundCount; i++) {
                try {
                    // Get a random wound texture for this tier
                    Identifier woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageTier, random);

                    // Load the wound texture from resource manager
                    net.minecraft.server.packs.resources.ResourceManager resourceManager =
                            net.minecraft.client.Minecraft.getInstance().getResourceManager();

                    NativeImage woundAsset;
                    try (var resource = resourceManager.open(woundAssetId)) {
                        woundAsset = NativeImage.read(resource);
                    }

                    // Random position for this wound (stay within bounds)
                    int maxX = baseWidth - woundAsset.getWidth();
                    int maxY = baseHeight - woundAsset.getHeight();
                    int posX = random.nextInt(maxX);
                    int posY = random.nextInt(maxY);

                    if (VisualHealth.debugMode) {
                        VisualHealth.LOGGER.debug("Stamping wound {} at ({}, {})", i + 1, posX, posY);
                    }

                    // Stamp the wound texture onto the base texture
                    stampTexture(woundTexture, woundAsset, posX, posY);

                } catch (Exception e) {
                    VisualHealth.LOGGER.error("Failed to load or stamp wound texture: {}", e.getMessage(), e);
                }
            }

            // Register the composite texture as a dynamic texture
            TextureManager textureManager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
            Identifier dynamicTextureId = Identifier.fromNamespaceAndPath("visualhealth",
                    "dynamic/wounds/" + entity.getId() + "/tier" + damageTier);

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Registering dynamic wound texture: {}", dynamicTextureId);
            }

            // Wrap the NativeImage in a DynamicTexture for GPU upload
            // Supplier provides the texture name for debugging
            DynamicTexture texture = new DynamicTexture(
                    () -> dynamicTextureId.toString(),
                    woundTexture
            );

            // Register the texture
            textureManager.register(dynamicTextureId, texture);

            return dynamicTextureId;

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate wound texture: {}", e.getMessage(), e);
            return getFallbackTexture();
        }
    }

    // Stamp a small texture onto a base texture at the specified position
    // Handles alpha blending for smooth wound edges
    private static void stampTexture(NativeImage baseTexture, NativeImage stamp, int posX, int posY) {
        for (int y = 0; y < stamp.getHeight(); y++) {
            for (int x = 0; x < stamp.getWidth(); x++) {
                // Check bounds
                if (posX + x >= baseTexture.getWidth() || posY + y >= baseTexture.getHeight()) {
                    continue;
                }

                // Get pixel from stamp texture (returns ARGB format)
                int stampPixel = stamp.getPixel(x, y);
                int stampAlpha = (stampPixel >> 24) & 0xFF;

                // Skip fully transparent pixels
                if (stampAlpha == 0) {
                    continue;
                }

                // Get existing pixel from base texture
                int baseX = posX + x;
                int baseY = posY + y;
                int basePixel = baseTexture.getPixel(baseX, baseY);
                int baseAlpha = (basePixel >> 24) & 0xFF;

                // Simple alpha blending (stamp overlays base)
                // If base is transparent, use stamp pixel directly
                if (baseAlpha == 0) {
                    baseTexture.setPixel(baseX, baseY, stampPixel);
                } else {
                    // Blend based on alpha
                    float alphaRatio = stampAlpha / 255.0f;
                    int blendedR = blendChannel((basePixel >> 16) & 0xFF, (stampPixel >> 16) & 0xFF, alphaRatio);
                    int blendedG = blendChannel((basePixel >> 8) & 0xFF, (stampPixel >> 8) & 0xFF, alphaRatio);
                    int blendedB = blendChannel(basePixel & 0xFF, stampPixel & 0xFF, alphaRatio);
                    int blendedA = Math.min(255, baseAlpha + stampAlpha);

                    int blendedPixel = (blendedA << 24) | (blendedR << 16) | (blendedG << 8) | blendedB;
                    baseTexture.setPixel(baseX, baseY, blendedPixel);
                }
            }
        }
    }

    // Blend two color channels based on alpha ratio
    private static int blendChannel(int base, int stamp, float alphaRatio) {
        return (int)(base * (1.0f - alphaRatio) + stamp * alphaRatio);
    }

    // Fallback texture if generation fails
    private static Identifier getFallbackTexture() {
        return Identifier.fromNamespaceAndPath("visualhealth", "damage/scratches/scratch1.png");
    }
}
