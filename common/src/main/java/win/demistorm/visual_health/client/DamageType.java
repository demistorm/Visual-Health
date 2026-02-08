package win.demistorm.visual_health.client;

// Types of damage that can be inflicted on an entity
// Each type corresponds to a wound texture folder and determines wound appearance
public enum DamageType {
    // Sharp cutting weapons (swords, shears)
    SWORD("sword", "sword"),

    // Heavy chopping weapons (axes, hatches)
    AXE("axe", "axe"),

    // Piercing weapons (tridents, spears, lances)
    TRIDENT("trident", "trident"),

    // Thrown piercing weapons (spears, thrown tridents)
    SPEAR("spear", "spear"),

    // Generic damage source (fallback for punches, magic, fire, fall, etc.)
    GENERIC("generic", "generic");

    private final String folderName;
    private final String texturePrefix;

    DamageType(String folderName, String texturePrefix) {
        this.folderName = folderName;
        this.texturePrefix = texturePrefix;
    }

    // Get the folder name for asset loading
    public String getFolderName() {
        return folderName;
    }

    // Get the texture prefix for file naming
    public String getTexturePrefix() {
        return texturePrefix;
    }
}
