package win.demistorm.visual_health.client.entitymappings;

import net.minecraft.world.entity.EntityType;
import static net.minecraft.world.entity.EntityType.*;
import win.demistorm.visual_health.VisualHealth;

import java.util.HashMap;
import java.util.Map;

// Entity-specific damage color overrides
public class EntityDamageColors {

    public record DamageOverride(int tintColor, boolean isEmissive) {
    }

    // Override wound colors
    private static final Map<EntityType<?>, DamageOverride> OVERRIDE_MAP = new HashMap<>();

    static {
        OC(CREEPER, "204020", false);
        OC(ENDERMAN, "D080FF", true);
        OC(ZOMBIE, "863B22", false);
        OC(SKELETON, "776E65", false);
        OC(STRAY, "776E65", false);
        OC(WITHER_SKELETON, "776E65", false);
        OC(WITHER, "776E65", false);
        OC(SKELETON_HORSE, "776E65", false);
        OC(SPIDER, "C4D3FF", false);
        OC(CAVE_SPIDER, "C4D3FF", false);
        OC(HUSK, "863B22", false);
        OC(ZOMBIE_HORSE, "863B22", false);
        OC(ZOMBIE, "863B22", false);
        OC(SKELETON, "776E65", false);
        OC(STRAY, "776E65", false);
        OC(WITHER_SKELETON, "776E65", false);
        OC(WITHER, "776E65", false);
        OC(SKELETON_HORSE, "776E65", false);
        OC(SPIDER, "C4D3FF", false);
        OC(CAVE_SPIDER, "C4D3FF", false);
        OC(HUSK, "863B22", false);
        OC(ZOMBIE_HORSE, "863B22", false);
        OC(ZOMBIE_VILLAGER, "863B22", false);
        OC(SQUID, "2323c1", false);
        OC(GLOW_SQUID, "00cdcd", false);
        OC(STRIDER, "e6e600", false);
        OC(PUFFERFISH, "ffffb7", false);
        OC(ZOMBIFIED_PIGLIN, "863B22", false);
        OC(GUARDIAN, "2323c1", false);
        OC(ELDER_GUARDIAN, "2323c1", false);
        OC(MAGMA_CUBE, "ddaf13", false);
        OC(SHULKER, "ae6984", false);
        OC(SLIME, "9fc08a", false);
        OC(WARDEN, "008388", false);

        VisualHealth.LOGGER.info("Loaded {} entity damage overrides", OVERRIDE_MAP.size());
    }

    private static void OC(EntityType<?> entityType, String colorHex, boolean isEmissive) {
        int rgb = Integer.parseInt(colorHex, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
        OVERRIDE_MAP.put(entityType, new DamageOverride(abgr, isEmissive));
    }

    public static DamageOverride getOverride(EntityType<?> entityType) {
        return OVERRIDE_MAP.get(entityType);
    }
}
