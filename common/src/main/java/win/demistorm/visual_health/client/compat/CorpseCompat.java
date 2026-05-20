package win.demistorm.visual_health.client.compat;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageRenderCheck;
import win.demistorm.visual_health.client.damagestate.TintCalculator;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;
import win.demistorm.visual_health.client.renderer.BufferSourceSwapHelper;
import win.demistorm.visual_health.client.renderer.RenderTypeHelper;
import win.demistorm.visual_health.client.texture.SkinColorSampler;
import win.demistorm.visual_health.client.texture.TintUtils;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

import java.util.Random;
import java.util.UUID;

public final class CorpseCompat {

    // 60% sword/20% axe/20% generic
    private static final DamageType[] WEIGHTED_TYPES = {
            DamageType.SWORD, DamageType.SWORD, DamageType.SWORD,
            DamageType.SWORD, DamageType.SWORD, DamageType.SWORD,
            DamageType.AXE, DamageType.AXE,
            DamageType.GENERIC, DamageType.GENERIC
    };

    private CorpseCompat() {}

    static final Class<?> CORPSE_ENTITY_CLASS;

    static {
        Class<?> cls = null;
        try {
            cls = Class.forName("de.maxhenkel.corpse.entities.CorpseEntity");
            VisualHealth.LOGGER.info("Corpse mod detected - corpse damage rendering enabled");
        } catch (ClassNotFoundException ignored) {
            // Corpse mod not installed
        }
        CORPSE_ENTITY_CLASS = cls;
    }

    public static boolean isCorpseEntity(net.minecraft.world.entity.Entity entity) {
        return CORPSE_ENTITY_CLASS != null && CORPSE_ENTITY_CLASS.isInstance(entity);
    }

    private static final ThreadLocal<UUID> CORPSE_UUID = new ThreadLocal<>();

    public static void setCorpseContext(UUID corpseEntityUUID) {
        CORPSE_UUID.set(corpseEntityUUID);
    }

    public static void clearCorpseContext() {
        CORPSE_UUID.remove();
    }

    public static boolean isCorpseInactive() {
        return CORPSE_UUID.get() == null;
    }

    public static UUID getCorpseEntityUUID() {
        return CORPSE_UUID.get();
    }

    public static RenderType handleCorpseTexture(RenderType renderType) {
        if (isCorpseInactive()) return null;
        if (!DamageRenderCheck.shouldRenderCorpseDamage()) return null;

        ResourceLocation texture = RenderTypeHelper.extractTexture(renderType);
        if (texture == null) return null;

        if (!isPlayerSkin(texture)) return null;

        if (RenderTypeHelper.shouldSkipRenderType(renderType)) return null;
        if (BufferSourceSwapHelper.shouldSkipTexture(texture)) return null;
        if (!ConfigHelper.INSTANCE.drawOnOptifineEmissives && texture.getPath().endsWith("_e.png")) return null;

        try {
            UUID corpseUUID = getCorpseEntityUUID();
            ResourceLocation replacement = generateCorpseTexture(corpseUUID, texture);

            if (replacement != null) {
                RenderType swapped = RenderTypeHelper.createWithTexture(renderType, texture, replacement);
                VisualHealth.LOGGER.debug("Corpse swap: {} -> {} for corpse {}", texture, replacement, corpseUUID);
                return swapped;
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to swap corpse texture: {}", e.getMessage(), e);
        }

        return null;
    }

    private static boolean isPlayerSkin(ResourceLocation texture) {
        return "minecraft".equals(texture.getNamespace()) && texture.getPath().startsWith("skins/");
    }

    private static int getTintForPlayerCorpse(DamageType damageType, ResourceLocation skinTexture) {
        int weaponTint = getPlayerCorpseWeaponTint(skinTexture);

        if (damageType == DamageType.GENERIC) {
            return TintUtils.blendColors(weaponTint, TintCalculator.BRUISE_BROWN, TintCalculator.BRUISE_BLEND_RATIO);
        }

        return weaponTint;
    }

    private static int getPlayerCorpseWeaponTint(ResourceLocation skinTexture) {
        EntityDamageColors.DamageOverride override = EntityDamageColors.getOverride(EntityType.PLAYER);
        if (override != null) {
            return override.tintColor();
        }

        if (ConfigHelper.INSTANCE.playerSampledDamage) {
            return SkinColorSampler.getSampledTint(skinTexture);
        }

        return switch (ConfigHelper.INSTANCE.damageColor) {
            case RED -> 0xFF00009F;
            case BLACK -> 0xFF000000;
            case WHITE -> 0xFFFFFFFF;
        };
    }

    private static ResourceLocation generateCorpseTexture(UUID corpseUUID, ResourceLocation baseTexture) {
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
