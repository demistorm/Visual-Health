package win.demistorm.visual_health.client.compat;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageRenderCheck;
import win.demistorm.visual_health.client.damagestate.TintCalculator;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;
import win.demistorm.visual_health.client.renderer.TextureSwapHelper;
import win.demistorm.visual_health.client.texture.SkinColorSampler;
import win.demistorm.visual_health.client.texture.TintUtils;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

import java.util.Random;
import java.util.UUID;

public final class CorpseCompat {

    private static final DamageType[] WEIGHTED_TYPES = {
            DamageType.SWORD, DamageType.SWORD, DamageType.SWORD,
            DamageType.SWORD, DamageType.SWORD, DamageType.SWORD,
            DamageType.AXE, DamageType.AXE,
            DamageType.GENERIC, DamageType.GENERIC
    };

    static final Class<?> DUMMY_PLAYER_CLASS;

    static {
        Class<?> cls = null;
        try {
            cls = Class.forName("de.maxhenkel.corpse.entities.DummyPlayer");
            VisualHealth.LOGGER.info("Corpse mod detected,  corpse damage rendering enabled");
        } catch (ClassNotFoundException ignored) {
        }
        DUMMY_PLAYER_CLASS = cls;
    }

    private CorpseCompat() {
    }

    private static final ThreadLocal<UUID> CORPSE_UUID = new ThreadLocal<>();

    public static boolean isDummyPlayer(LivingEntity entity) {
        return DUMMY_PLAYER_CLASS != null && DUMMY_PLAYER_CLASS.isInstance(entity);
    }

    public static void setCorpseContext(UUID uuid) {
        CORPSE_UUID.set(uuid);
    }

    public static void clearCorpseContext() {
        CORPSE_UUID.remove();
    }

    public static boolean isCorpseActive() {
        return CORPSE_UUID.get() != null;
    }

    public static Identifier swapCorpseTexture(Identifier texture) {
        if (!isCorpseActive()) return null;
        if (!DamageRenderCheck.shouldRenderCorpseDamage()) return null;
        if (!isPlayerSkin(texture)) return null;
        if (TextureSwapHelper.shouldSkipTexture(texture)) return null;
        if (!ConfigHelper.INSTANCE.drawOnOptifineEmissives && texture.getPath().endsWith("_e.png")) return null;

        try {
            UUID corpseUUID = CORPSE_UUID.get();
            Identifier replacement = generateCorpseTexture(corpseUUID, texture);

            if (replacement != null) {
                VisualHealth.LOGGER.debug("Corpse swap: {} -> {} for corpse {}", texture, replacement, corpseUUID);
                return replacement;
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to swap corpse texture: {}", e.getMessage(), e);
        }

        return null;
    }

    private static boolean isPlayerSkin(Identifier texture) {
        return "minecraft".equals(texture.getNamespace()) && texture.getPath().startsWith("skins/");
    }

    private static int getTintForPlayerCorpse(DamageType damageType, Identifier skinTexture) {
        int weaponTint = getPlayerCorpseWeaponTint(skinTexture);

        if (damageType == DamageType.GENERIC) {
            return TintUtils.blendColors(weaponTint, TintCalculator.BRUISE_BROWN, TintCalculator.BRUISE_BLEND_RATIO);
        }

        return weaponTint;
    }

    private static int getPlayerCorpseWeaponTint(Identifier skinTexture) {
        EntityDamageColors.DamageOverride override = EntityDamageColors.getOverride(EntityTypes.PLAYER);
        if (override != null) {
            return override.tintColor();
        }

        if (ConfigHelper.INSTANCE.playerSampledDamage) {
            return SkinColorSampler.getSampledTint(skinTexture);
        }

        return switch (ConfigHelper.INSTANCE.damageColor) {
            case RED -> 0xFF9F0000;
            case BLACK -> 0xFF000000;
            case WHITE -> 0xFFFFFFFF;
        };
    }

    private static Identifier generateCorpseTexture(UUID corpseUUID, Identifier baseTexture) {
        DamageType[] damageTypes = generateCorpseDamageTypes(corpseUUID);

        return WoundTextureGenerator.builder()
                .category("corpse")
                .ownerId("corpse:" + corpseUUID)
                .damageTier(damageTypes.length)
                .texture(baseTexture)
                .composite()
                .damageTypes(damageTypes)
                .seed(corpseUUID.getLeastSignificantBits())
                .tint(type -> getTintForPlayerCorpse(type, baseTexture))
                .generate();
    }

    private static DamageType[] generateCorpseDamageTypes(UUID corpseUUID) {
        int tierCount = ConfigHelper.INSTANCE.damageTierCount;
        DamageType[] types = new DamageType[tierCount];
        Random random = new Random(corpseUUID.getLeastSignificantBits());
        for (int i = 0; i < tierCount; i++) {
            types[i] = WEIGHTED_TYPES[random.nextInt(WEIGHTED_TYPES.length)];
        }
        return types;
    }
}
