package win.demistorm.visual_health.client.renderer;

import net.minecraft.resources.Identifier;
import win.demistorm.visual_health.VisualHealth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// Selects wound texture identifiers based on damage tier
// Returns texture IDs for RenderLayer system, not actual image data
public class WoundAssetSelector {

    private static final String MODID = "visualhealth";
    private static final String TEXTURE_FOLDER = "damage";

    // Cached texture identifiers organized by wound type
    private static Map<WoundType, List<Identifier>> woundTextures = new HashMap<>();
    private static boolean texturesLoaded = false;

    // Wound types matching our asset folder structure
    public enum WoundType {
        SCRATCH("scratches"),
        CUT("cuts"),
        WOUND("wounds"),
        DRIP("drips");

        final String folderName;

        WoundType(String folderName) {
            this.folderName = folderName;
        }
    }

    // Load texture identifiers (not actual image data)
    public static void loadTextures() {
        VisualHealth.LOGGER.info("Loading wound texture identifiers for Visual Health");

        // Load texture identifiers for each wound type
        woundTextures.put(WoundType.SCRATCH, loadTexturesFromFolder(WoundType.SCRATCH.folderName));
        woundTextures.put(WoundType.CUT, loadTexturesFromFolder(WoundType.CUT.folderName));
        woundTextures.put(WoundType.WOUND, loadTexturesFromFolder(WoundType.WOUND.folderName));
        woundTextures.put(WoundType.DRIP, loadTexturesFromFolder(WoundType.DRIP.folderName));

        int totalTextures = 0;
        for (List<Identifier> list : woundTextures.values()) {
            totalTextures += list.size();
        }

        VisualHealth.LOGGER.info("Loaded {} wound texture identifiers (scratches: {}, cuts: {}, wounds: {}, drips: {})",
                totalTextures,
                woundTextures.get(WoundType.SCRATCH).size(),
                woundTextures.get(WoundType.CUT).size(),
                woundTextures.get(WoundType.WOUND).size(),
                woundTextures.get(WoundType.DRIP).size());

        texturesLoaded = true;
    }

    // Load texture identifiers from a specific folder
    // We assume textures are named: scratch1.png, scratch2.png, etc.
    private static List<Identifier> loadTexturesFromFolder(String folderName) {
        List<Identifier> textures = new ArrayList<>();

        // Try to load textures numbered 1-20 (covers most cases)
        for (int i = 1; i <= 20; i++) {
            Identifier textureId = Identifier.fromNamespaceAndPath(MODID,
                    TEXTURE_FOLDER + "/" + folderName + "/" + folderName.substring(0, folderName.length() - 1) + i + ".png");

            // Note: We don't check if the texture exists here
            // The rendering system will handle missing textures gracefully
            textures.add(textureId);
        }

        return textures;
    }

    // Get a random wound texture identifier based on damage tier
    public static Identifier getRandomWoundTexture(int damageTier, Random random) {
        // Lazy loading
        if (!texturesLoaded) {
            loadTextures();
        }

        // Select wound type based on damage tier
        WoundType woundType = selectWoundType(damageTier, random);

        // Get textures for this type
        List<Identifier> textures = woundTextures.get(woundType);
        if (textures == null || textures.isEmpty()) {
            VisualHealth.LOGGER.warn("No textures found for wound type: {}", woundType);
            return getFallbackTexture();
        }

        // Return random texture from the list
        return textures.get(random.nextInt(textures.size()));
    }

    // Select wound type based on damage tier with some randomness
    private static WoundType selectWoundType(int damageTier, Random random) {
        return switch (damageTier) {
            case 1 -> // Light damage: mostly scratches, some cuts
                    random.nextFloat() < 0.7f ? WoundType.SCRATCH : WoundType.CUT;
            case 2 -> // Moderate damage: mix of cuts and scratches
                    random.nextFloat() < 0.5f ? WoundType.CUT : WoundType.SCRATCH;
            case 3 -> // Heavy damage: mostly wounds, some cuts
                    random.nextFloat() < 0.6f ? WoundType.WOUND : WoundType.CUT;
            case 4 -> // Critical damage: wounds and drips
                    random.nextFloat() < 0.5f ? WoundType.WOUND :
                            (random.nextFloat() < 0.5f ? WoundType.DRIP : WoundType.WOUND);
            default -> WoundType.SCRATCH;
        };
    }

    // Fallback texture if none found
    private static Identifier getFallbackTexture() {
        return Identifier.fromNamespaceAndPath(MODID, "textures/damage/scratches/scratch1.png");
    }

    // Clean up on resource reload
    public static void cleanup() {
        VisualHealth.LOGGER.info("Cleaning up wound texture identifiers");
        texturesLoaded = false;
        woundTextures.clear();
    }
}
