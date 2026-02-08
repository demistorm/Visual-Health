package win.demistorm.visual_health.client.renderer;

import net.minecraft.resources.Identifier;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// Selects wound texture identifiers based on damage type
// Returns texture IDs for RenderLayer system, not actual image data
public class WoundAssetSelector {

    private static final String MODID = "visualhealth";
    private static final String TEXTURE_FOLDER = "damage";

    // Cached texture identifiers organized by damage type
    private static Map<DamageType, List<Identifier>> woundTextures = new HashMap<>();
    private static boolean texturesLoaded = false;

    // Load texture identifiers (not actual image data)
    public static void loadTextures() {
        VisualHealth.LOGGER.info("Loading wound texture identifiers for Visual Health");

        // Load texture identifiers for each damage type
        for (DamageType damageType : DamageType.values()) {
            woundTextures.put(damageType, loadTexturesFromFolder(damageType));
        }

        int totalTextures = 0;
        for (List<Identifier> list : woundTextures.values()) {
            totalTextures += list.size();
        }

        VisualHealth.LOGGER.info("Loaded {} wound texture identifiers across {} damage types",
                totalTextures, DamageType.values().length);

        texturesLoaded = true;
    }

    // Load texture identifiers from a specific folder
    // We assume textures are named: sword1.png, axe1.png, etc.
    private static List<Identifier> loadTexturesFromFolder(DamageType damageType) {
        List<Identifier> textures = new ArrayList<>();

        // Load texture #1 (we only have one texture per type for now)
        for (int i = 1; i <= 1; i++) {
            Identifier textureId = Identifier.fromNamespaceAndPath(MODID,
                    TEXTURE_FOLDER + "/" + damageType.getFolderName() + "/" + damageType.getTexturePrefix() + i + ".png");

            // Note: We don't check if the texture exists here
            // The rendering system will handle missing textures gracefully
            textures.add(textureId);
        }

        return textures;
    }

    // Get a random wound texture identifier based on damage type
    public static Identifier getRandomWoundTexture(DamageType damageType, Random random) {
        // Lazy loading
        if (!texturesLoaded) {
            loadTextures();
        }

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Getting wound texture for damage type: {}", damageType);
        }

        // Get textures for this type
        List<Identifier> textures = woundTextures.get(damageType);
        if (textures == null || textures.isEmpty()) {
            VisualHealth.LOGGER.warn("No textures found for damage type: {}", damageType);
            return getFallbackTexture();
        }

        // Return random texture from the list
        Identifier texture = textures.get(random.nextInt(textures.size()));

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Selected texture: {} (from {} textures)",
                    texture, textures.size());
        }

        return texture;
    }

    // Fallback texture if none found
    private static Identifier getFallbackTexture() {
        return Identifier.fromNamespaceAndPath(MODID, "damage/generic/generic1.png");
    }

    // Clean up on resource reload
    public static void cleanup() {
        VisualHealth.LOGGER.info("Cleaning up wound texture identifiers");
        texturesLoaded = false;
        woundTextures.clear();
    }
}
