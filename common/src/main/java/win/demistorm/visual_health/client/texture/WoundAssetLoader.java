package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.Logger;
import win.demistorm.visual_health.VisualHealth;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Loads and caches greyscale wound assets from the damage folders
public class WoundAssetLoader {

    private static final Logger LOGGER = VisualHealth.LOGGER;

    // Asset folder paths
    private static final String DAMAGE_FOLDER = "damage";
    private static final String SCRATCHES_FOLDER = "scratches";
    private static final String CUTS_FOLDER = "cuts";
    private static final String WOUNDS_FOLDER = "wounds";
    private static final String DRIPS_FOLDER = "drips";

    // Loaded wound assets organized by type
    private static List<NativeImage> scratches = new ArrayList<>();
    private static List<NativeImage> cuts = new ArrayList<>();
    private static List<NativeImage> wounds = new ArrayList<>();
    private static List<NativeImage> drips = new ArrayList<>();

    // All assets combined for random selection
    private static Map<WoundType, List<NativeImage>> woundAssets = new HashMap<>();

    // Flag to track if assets have been loaded
    private static boolean assetsLoaded = false;

    // Enum for wound types to organize assets
    public enum WoundType {
        SCRATCH,
        CUT,
        WOUND,
        DRIP
    }

    // Load all wound assets at startup
    public static void loadAssets() {
        LOGGER.info("Loading wound assets for Visual Health");

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager == null) {
            LOGGER.warn("ResourceManager not available yet, skipping asset load (will load on first use)");
            return;
        }

        scratches = loadAssetsFromFolder(resourceManager, SCRATCHES_FOLDER);
        cuts = loadAssetsFromFolder(resourceManager, CUTS_FOLDER);
        wounds = loadAssetsFromFolder(resourceManager, WOUNDS_FOLDER);
        drips = loadAssetsFromFolder(resourceManager, DRIPS_FOLDER);

        woundAssets.put(WoundType.SCRATCH, scratches);
        woundAssets.put(WoundType.CUT, cuts);
        woundAssets.put(WoundType.WOUND, wounds);
        woundAssets.put(WoundType.DRIP, drips);

        int totalAssets = scratches.size() + cuts.size() + wounds.size() + drips.size();
        LOGGER.info("Loaded {} wound assets (scratches: {}, cuts: {}, wounds: {}, drips: {})",
                totalAssets, scratches.size(), cuts.size(), wounds.size(), drips.size());

        assetsLoaded = true;
    }

    // Load all PNG assets from a specific folder
    private static List<NativeImage> loadAssetsFromFolder(ResourceManager resourceManager, String folderName) {
        List<NativeImage> assets = new ArrayList<>();
        String folderPath = DAMAGE_FOLDER + "/" + folderName + "/";

        // Use new 1.21.11 ResourceManager.listResources API
        // listResources(String prefix, Predicate<Identifier> predicate) returns Map<Identifier, Resource>
        Map<Identifier, Resource> resourceMap = resourceManager.listResources(
                folderPath,
                location -> location.getPath().endsWith(".png")
        );

        // Load all resources from the map
        for (Map.Entry<Identifier, Resource> entry : resourceMap.entrySet()) {
            Identifier location = entry.getKey();
            Resource resource = entry.getValue();
            try (InputStream stream = resource.open()) {
                NativeImage image = NativeImage.read(stream);
                assets.add(image);
                LOGGER.debug("Loaded wound asset: {}", location);
            } catch (IOException e) {
                LOGGER.error("Failed to load wound asset: {}", location, e);
            }
        }

        return assets;
    }

    // Get a random wound asset of a specific type
    public static NativeImage getRandomWound(WoundType type) {
        // Lazy loading - load assets on first use
        if (!assetsLoaded) {
            loadAssets();
        }

        List<NativeImage> assets = woundAssets.get(type);
        if (assets == null || assets.isEmpty()) {
            return null;
        }
        return assets.get((int) (Math.random() * assets.size()));
    }

    // Get all wound assets (for verification/debugging)
    public static Map<WoundType, List<NativeImage>> getAllAssets() {
        return woundAssets;
    }

    // Clean up assets when shutting down or reloading resources
    public static void cleanup() {
        LOGGER.info("Cleaning up wound assets");
        assetsLoaded = false; // Reset flag so assets can be reloaded

        for (List<NativeImage> assetList : woundAssets.values()) {
            for (NativeImage image : assetList) {
                if (image != null) {
                    image.close();
                }
            }
            assetList.clear();
        }

        scratches.clear();
        cuts.clear();
        wounds.clear();
        drips.clear();
    }
}
