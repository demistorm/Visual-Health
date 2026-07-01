package win.demistorm.visual_health.client.entitymappings;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
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
        TEXTURE_SIZES.put(EntityTypes.ALLAY, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityTypes.ARMADILLO, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.BAT, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityTypes.CAT, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.CHICKEN, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.COD, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityTypes.COW, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.DONKEY, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.FOX, new TextureSize(48, 32));
        TEXTURE_SIZES.put(EntityTypes.FROG, new TextureSize(48, 48));
        TEXTURE_SIZES.put(EntityTypes.GLOW_SQUID, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.HORSE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.MOOSHROOM, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.MULE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.OCELOT, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.PARROT, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityTypes.PIG, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.RABBIT, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.SHEEP, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.SKELETON_HORSE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.SNIFFER, new TextureSize(192, 192));
        TEXTURE_SIZES.put(EntityTypes.SQUID, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.STRIDER, new TextureSize(64, 128));
        TEXTURE_SIZES.put(EntityTypes.TADPOLE, new TextureSize(16, 16));
        TEXTURE_SIZES.put(EntityTypes.TROPICAL_FISH, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityTypes.TURTLE, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityTypes.VILLAGER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.PUFFERFISH, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityTypes.SALMON, new TextureSize(32, 32));

        // Misc entities
        TEXTURE_SIZES.put(EntityTypes.CREAKING, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.SNOW_GOLEM, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.WANDERING_TRADER, new TextureSize(64, 64));

        // Neutral mobs
        TEXTURE_SIZES.put(EntityTypes.BEE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.CAVE_SPIDER, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.DOLPHIN, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.ENDERMAN, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.GOAT, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.GUARDIAN, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.IRON_GOLEM, new TextureSize(128, 128));
        TEXTURE_SIZES.put(EntityTypes.LLAMA, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityTypes.PANDA, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.POLAR_BEAR, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityTypes.SPIDER, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.WOLF, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.ZOMBIFIED_PIGLIN, new TextureSize(64, 64));

        // Hostile Mobs
        TEXTURE_SIZES.put(EntityTypes.BLAZE, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.BOGGED, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.BREEZE, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityTypes.CREEPER, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.DROWNED, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.ELDER_GUARDIAN, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.ENDERMITE, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.EVOKER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.GHAST, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityTypes.HAPPY_GHAST, new TextureSize(128, 128));
        TEXTURE_SIZES.put(EntityTypes.HOGLIN, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityTypes.HUSK, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.ILLUSIONER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.MAGMA_CUBE, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.PHANTOM, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.PIGLIN, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.PIGLIN_BRUTE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.PILLAGER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.RAVAGER, new TextureSize(128, 128));
        TEXTURE_SIZES.put(EntityTypes.SHULKER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.SILVERFISH, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.SKELETON, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.SLIME, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.STRAY, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.VEX, new TextureSize(32, 32));
        TEXTURE_SIZES.put(EntityTypes.VINDICATOR, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.WARDEN, new TextureSize(128, 128));
        TEXTURE_SIZES.put(EntityTypes.WITCH, new TextureSize(64, 128));
        TEXTURE_SIZES.put(EntityTypes.WITHER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.WITHER_SKELETON, new TextureSize(64, 32));
        TEXTURE_SIZES.put(EntityTypes.ZOGLIN, new TextureSize(128, 64));
        TEXTURE_SIZES.put(EntityTypes.ZOMBIE, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.ZOMBIE_VILLAGER, new TextureSize(64, 64));
        TEXTURE_SIZES.put(EntityTypes.ENDER_DRAGON, new TextureSize(256, 256));

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
