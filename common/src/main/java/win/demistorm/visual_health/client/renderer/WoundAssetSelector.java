package win.demistorm.visual_health.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.DamageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

// Select random wound textures by damage type
public class WoundAssetSelector {

    private static final String MODID = "visualhealth";
    private static final String TEXTURE_FOLDER = "damage";

    private static final Map<DamageType, List<Identifier>> woundTextures = new HashMap<>();
    private static boolean texturesLoaded = false;

    public static void loadTextures() {
        VisualHealth.LOGGER.info("Loading wound texture identifiers for Visual Health");

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

    private static List<Identifier> loadTexturesFromFolder(DamageType damageType) {

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String folderPath = TEXTURE_FOLDER + "/" + damageType.getFolderName();

        Predicate<Identifier> predicate = id ->
                id.getNamespace().equals("visualhealth") &&
                        id.getPath().startsWith(folderPath + "/") &&
                        id.getPath().endsWith(".png");

        Map<Identifier, Resource> foundResources = resourceManager.listResources(folderPath, predicate);
        List<Identifier> textures = new ArrayList<>(foundResources.keySet());

        VisualHealth.LOGGER.debug("Found {} textures in folder {} for damage type {}",
                textures.size(), folderPath, damageType);

        return textures;
    }

    // Get a random wound texture based on damage type
    public static Identifier getRandomWoundTexture(DamageType damageType, Random random) {
        if (!texturesLoaded) {
            loadTextures();
        }

        List<Identifier> textures = woundTextures.get(damageType);
        if (textures == null || textures.isEmpty()) {
            VisualHealth.LOGGER.warn("No textures found for damage type: {}", damageType);
            return getFallbackTexture();
        }

        Identifier texture = textures.get(random.nextInt(textures.size()));

        VisualHealth.LOGGER.debug("Selected texture: {} (from {} textures)",
                texture, textures.size());

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
