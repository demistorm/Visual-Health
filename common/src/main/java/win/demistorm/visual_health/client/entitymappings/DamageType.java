package win.demistorm.visual_health.client.entitymappings;

// Damage types determine wound texture
public enum DamageType {
    SWORD("sword", "sword"),
    AXE("axe", "axe"),
    TRIDENT("trident", "trident"),
    SPEAR("spear", "spear"),
    GENERIC("generic", "generic");

    private final String folderName;
    private final String texturePrefix;

    DamageType(String folderName, String texturePrefix) {
        this.folderName = folderName;
        this.texturePrefix = texturePrefix;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getTexturePrefix() {
        return texturePrefix;
    }
}
