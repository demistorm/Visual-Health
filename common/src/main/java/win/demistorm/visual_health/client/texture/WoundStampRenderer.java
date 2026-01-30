package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import org.apache.logging.log4j.Logger;
import win.demistorm.visual_health.VisualHealth;

import java.util.Random;

// Handles stamping wound assets onto textures with color tinting and blending
public class WoundStampRenderer {

    private static final Logger LOGGER = VisualHealth.LOGGER;

    // Color tints for different texture types (ARGB format)
    // Blood red for base textures
    public static final int BLOOD_TINT = 0xFFFF0000;
    // Dark gray for emissive textures (glowing parts don't bleed bright red)
    public static final int EMISSIVE_TINT = 0xFF202020;

    // Stamp a single wound onto the target texture at a random position
    public static void stampWound(NativeImage targetTexture, NativeImage woundAsset,
                                   int tintArgb, long entityId, int woundIndex) {
        if (woundAsset == null || targetTexture == null) {
            return;
        }

        // Create seeded random for consistent wound placement per entity
        Random random = new Random(entityId + woundIndex * 31L);

        // Random position on texture
        int maxX = targetTexture.getWidth() - woundAsset.getWidth();
        int maxY = targetTexture.getHeight() - woundAsset.getHeight();

        if (maxX < 0 || maxY < 0) {
            // Wound asset is larger than texture, skip it
            return;
        }

        int x = random.nextInt(maxX);
        int y = random.nextInt(maxY);

        // Random rotation (0, 90, 180, 270 degrees)
        int rotation = random.nextInt(4);

        // Random scale (0.8x to 1.2x)
        float scale = 0.8f + random.nextFloat() * 0.4f;

        // Apply the wound with transformations
        applyWoundWithTransform(targetTexture, woundAsset, x, y, rotation, scale, tintArgb, random);
    }

    // Apply wound with rotation, scaling, and color tinting
    private static void applyWoundWithTransform(NativeImage target, NativeImage wound,
                                                 int x, int y, int rotation, float scale,
                                                 int tintArgb, Random random) {
        int woundWidth = wound.getWidth();
        int woundHeight = wound.getHeight();

        // Calculate scaled dimensions
        int scaledWidth = (int) (woundWidth * scale);
        int scaledHeight = (int) (woundHeight * scale);

        // Extract tint color components
        int tintAlpha = (tintArgb >> 24) & 0xFF;
        int tintRed = (tintArgb >> 16) & 0xFF;
        int tintGreen = (tintArgb >> 8) & 0xFF;
        int tintBlue = tintArgb & 0xFF;

        // Apply each pixel from wound to target with transformations
        for (int dy = 0; dy < scaledHeight; dy++) {
            for (int dx = 0; dx < scaledWidth; dx++) {
                // Calculate source coordinates with rotation
                int srcX = (int) (dx / scale);
                int srcY = (int) (dy / scale);

                // Apply rotation
                int rotX = srcX;
                int rotY = srcY;

                switch (rotation) {
                    case 1: // 90 degrees
                        rotX = woundHeight - srcY - 1;
                        rotY = srcX;
                        break;
                    case 2: // 180 degrees
                        rotX = woundWidth - srcX - 1;
                        rotY = woundHeight - srcY - 1;
                        break;
                    case 3: // 270 degrees
                        rotX = srcY;
                        rotY = woundWidth - srcX - 1;
                        break;
                }

                // Check bounds
                if (rotX < 0 || rotX >= woundWidth || rotY < 0 || rotY >= woundHeight) {
                    continue;
                }

                // Get target position
                int targetX = x + dx;
                int targetY = y + dy;

                if (targetX < 0 || targetX >= target.getWidth() ||
                        targetY < 0 || targetY >= target.getHeight()) {
                    continue;
                }

                // Get wound pixel (greyscale) - using access widener
                int woundPixel = wound.getPixelABGR(rotX, rotY);
                int woundAlpha = (woundPixel >> 24) & 0xFF;

                // Skip fully transparent pixels
                if (woundAlpha == 0) {
                    continue;
                }

                // Get luminance from greyscale wound (R=G=B for greyscale)
                int luminance = ((woundPixel >> 16) & 0xFF);

                // Apply color tint while preserving luminance as brightness
                // Brighter areas of wound become brighter red, darker become darker red
                int tintedRed = Math.min(255, (luminance * tintRed) / 128);
                int tintedGreen = Math.min(255, (luminance * tintGreen) / 128);
                int tintedBlue = Math.min(255, (luminance * tintBlue) / 128);

                // Blend with existing pixel using alpha compositing
                int targetPixel = target.getPixelABGR(targetX, targetY);

                int blendedRed = blendColors(
                        (targetPixel >> 16) & 0xFF,
                        tintedRed,
                        woundAlpha / 255.0f * 0.7f // 70% opacity for wounds
                );
                int blendedGreen = blendColors(
                        (targetPixel >> 8) & 0xFF,
                        tintedGreen,
                        woundAlpha / 255.0f * 0.7f
                );
                int blendedBlue = blendColors(
                        targetPixel & 0xFF,
                        tintedBlue,
                        woundAlpha / 255.0f * 0.7f
                );

                // Write blended pixel - using access widener
                int blendedPixel = (0xFF << 24) | (blendedRed << 16) | (blendedGreen << 8) | blendedBlue;
                target.setPixelABGR(targetX, targetY, blendedPixel);
            }
        }
    }

    // Blend two colors with given alpha (standard alpha blending)
    private static int blendColors(int original, int tint, float alpha) {
        // Linear interpolation between original and tint
        return (int) (original * (1.0f - alpha) + tint * alpha);
    }

    // Apply multiple wounds to a texture based on damage tier
    public static void applyWoundsByTier(NativeImage targetTexture, int damageTier,
                                          int tintArgb, long entityId) {
        if (damageTier == 0) {
            return; // No damage
        }

        int woundCount = HealthTierCalculator.getWoundCount(damageTier);

        for (int i = 0; i < woundCount; i++) {
            // Select wound type based on damage tier
            WoundAssetLoader.WoundType woundType = selectWoundType(damageTier, i);
            NativeImage woundAsset = WoundAssetLoader.getRandomWound(woundType);

            if (woundAsset != null) {
                stampWound(targetTexture, woundAsset, tintArgb, entityId, i);
            }
        }
    }

    // Select appropriate wound type based on damage tier and wound index
    private static WoundAssetLoader.WoundType selectWoundType(int damageTier, int woundIndex) {
        // Mix wound types based on tier
        // Lower tiers: mostly scratches and cuts
        // Higher tiers: more wounds and drips

        switch (damageTier) {
            case 1: // Light damage - mostly scratches
                return woundIndex % 3 == 0 ? WoundAssetLoader.WoundType.CUT :
                        WoundAssetLoader.WoundType.SCRATCH;
            case 2: // Moderate damage - mix of scratches and cuts
                return woundIndex % 2 == 0 ? WoundAssetLoader.WoundType.CUT :
                        WoundAssetLoader.WoundType.SCRATCH;
            case 3: // Heavy damage - cuts and wounds
                return woundIndex % 3 == 0 ? WoundAssetLoader.WoundType.WOUND :
                        WoundAssetLoader.WoundType.CUT;
            case 4: // Critical - everything including drips
                int type = woundIndex % 5;
                if (type == 0) return WoundAssetLoader.WoundType.DRIP;
                if (type <= 2) return WoundAssetLoader.WoundType.WOUND;
                return WoundAssetLoader.WoundType.CUT;
            default:
                return WoundAssetLoader.WoundType.SCRATCH;
        }
    }
}
