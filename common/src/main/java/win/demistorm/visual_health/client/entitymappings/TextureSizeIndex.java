package win.demistorm.visual_health.client.entitymappings;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.client.texture.AlphaMaskCache;
import win.demistorm.visual_health.client.texture.TextureLocator;
import win.demistorm.visual_health.client.texture.TextureSize;
import win.demistorm.visual_health.VisualHealth;

import java.util.HashMap;
import java.util.Map;

public class TextureSizeIndex {

    private static final Map<EntityType<?>, TextureSize> TEXTURE_SIZES = new HashMap<>();

    private static final TextureSize DEFAULT_SIZE = new TextureSize(64, 64);

    static {
        // Passive mobs
        TEXTURE_SIZES.put(EntityType.ALLAY, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityType.ARMADILLO, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.BAT, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityType.CAT, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.CHICKEN, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.COD, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityType.COW, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.DONKEY, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.FOX, new TextureSize(48, 32));
        TEXTURE_SIZES.put(EntityType.FROG, new TextureSize(48, 48));
        TEXTURE_SIZES.put(EntityType.GLOW_SQUID, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.HORSE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.MOOSHROOM, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.MULE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.OCELOT, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.PARROT, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityType.PIG, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.RABBIT, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.SHEEP, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.SKELETON_HORSE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.SNIFFER, new TextureSize(192, 192));
        TEXTURE_SIZES.put(EntityType.SQUID, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.STRIDER, new TextureSize(64, 128));
        TEXTURE_SIZES.put(EntityType.TADPOLE, new TextureSize(16, 16));
        TEXTURE_SIZES.put(EntityType.TROPICAL_FISH, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityType.TURTLE, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityType.VILLAGER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.PUFFERFISH, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityType.SALMON, new TextureSize(32, 32));

        // Misc entities
        TEXTURE_SIZES.put(EntityType.CREAKING, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.SNOW_GOLEM, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.WANDERING_TRADER, new TextureSize(64, 64));

        // Neutral mobs
        TEXTURE_SIZES.put(EntityType.BEE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.CAVE_SPIDER, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.DOLPHIN, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.ENDERMAN, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.GOAT, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.GUARDIAN, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.IRON_GOLEM, new TextureSize(128, 128));
        TEXTURE_SIZES.put(EntityType.LLAMA, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityType.PANDA, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.POLAR_BEAR, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityType.SPIDER, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.WOLF, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.ZOMBIFIED_PIGLIN, new TextureSize(64, 64));

        // Hostile Mobs
        TEXTURE_SIZES.put(EntityType.BLAZE, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.BOGGED, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.BREEZE, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityType.CREEPER, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.DROWNED, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.ELDER_GUARDIAN, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.ENDERMITE, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.EVOKER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.GHAST, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityType.HAPPY_GHAST, new TextureSize(128, 128));
        TEXTURE_SIZES.put(EntityType.HOGLIN, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityType.HUSK, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.ILLUSIONER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.MAGMA_CUBE, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.PHANTOM, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.PIGLIN, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.PIGLIN_BRUTE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.PILLAGER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.RAVAGER, new TextureSize(128, 128));
        TEXTURE_SIZES.put(EntityType.SHULKER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.SILVERFISH, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.SKELETON, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.SLIME, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.STRAY, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.VEX, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityType.VINDICATOR, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.WARDEN, new TextureSize(128, 128));
        TEXTURE_SIZES.put(EntityType.WITCH, new TextureSize(64, 128));
        TEXTURE_SIZES.put(EntityType.WITHER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.WITHER_SKELETON, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityType.ZOGLIN, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityType.ZOMBIE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.ZOMBIE_VILLAGER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityType.ENDER_DRAGON, new TextureSize(256, 256));

        VisualHealth.LOGGER.info("Loaded {} entity texture sizes", TEXTURE_SIZES.size());
    }

    public static TextureSize getTextureSize(LivingEntity entity) {
        TextureSize cachedSize = TEXTURE_SIZES.get(entity.getType());
        if (cachedSize != null) {
            VisualHealth.LOGGER.debug("Loaded hardcoded texture size");
            return cachedSize;
        }

        try {
            Identifier textureId = TextureLocator.getEntityTexture(entity);
            if (textureId != null) {
                TextureSize dynamicSize = AlphaMaskCache.getOrGenerateTextureSize(textureId);
                if (dynamicSize != null) {
                    TEXTURE_SIZES.put(entity.getType(), dynamicSize);

                    VisualHealth.LOGGER.debug("Dynamically detected texture size {}x{} for {} (ID: {})",
                            dynamicSize.width(), dynamicSize.height(),
                            entity.getName().getString(), entity.getId());

                    return dynamicSize;
                }
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.warn("Failed to dynamically detect texture size for {}: {}",
                    entity.getName().getString(), e.getMessage());
        }

        // Final fallback (default to 64x64)
        VisualHealth.LOGGER.debug("Using default texture size 64x64 for {} (not in index and dynamic detection failed)",
                entity.getName().getString());

        return DEFAULT_SIZE;
    }

    private TextureSizeIndex() {
    }
}
