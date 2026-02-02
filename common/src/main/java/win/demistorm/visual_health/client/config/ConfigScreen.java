package win.demistorm.visual_health.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;

// Main configuration screen for Visual Health
// Follows VR Throwing Extensions design pattern for consistency
public final class ConfigScreen {

    private ConfigScreen() {
        // Utility class - no instances
    }

    public static class VisualHealthConfigScreen extends Screen {
        private final Screen parent;
        private final Minecraft client = Minecraft.getInstance();

        // Track current values for UI
        private int woundsPerTierValue = ConfigHelper.INSTANCE.woundsPerTier;
        private boolean damagePassiveMobsValue = ConfigHelper.INSTANCE.damagePassiveMobs;
        private boolean damageVillagersValue = ConfigHelper.INSTANCE.damageVillagers;
        private ConfigHelper.DamageColor damageColorValue = ConfigHelper.INSTANCE.damageColor;

        // UI widgets
        private VisualHealthConfigSlider woundsSlider;
        private Button passiveMobsButton;
        private Button villagersButton;
        private Button colorButton;

        protected VisualHealthConfigScreen(Screen parent) {
            super(Component.literal("Visual Health Configuration"));
            this.parent = parent;
        }

        // Create screen for ModMenu
        public static VisualHealthConfigScreen create(Screen parent) {
            return new VisualHealthConfigScreen(parent);
        }

        @Override
        protected void init() {
            // Reset config button (top right, 45x20)
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Reset"),
                                    btn -> {
                                        // Reset all config values to defaults
                                        woundsPerTierValue = 6;
                                        damagePassiveMobsValue = true;
                                        damageVillagersValue = false;
                                        damageColorValue = ConfigHelper.DamageColor.RED;

                                        // Update UI immediately
                                        woundsSlider.setValue(6);
                                        updatePassiveMobsButton();
                                        updateVillagersButton();
                                        updateColorButton();
                                    })
                            .bounds(width - 50, 5, 45, 20)
                            .tooltip(Tooltip.create(Component.literal("Reset all settings to default")))
                            .build());

            // Slider for wounds per tier (1-10)
            woundsSlider = new VisualHealthConfigSlider(
                    width / 2 - 80,  // x position (centered)
                    height / 6 - 10, // y position
                    160,             // width
                    20,              // height
                    1,               // min value
                    10,              // max value
                    woundsPerTierValue, // current value
                    value -> Component.literal("Wounds per Tier: " + value), // message formatter
                    value -> {
                        woundsPerTierValue = value; // Update tracked value
                    }
            );
            addRenderableWidget(woundsSlider);

            // Toggle for passive mobs
            passiveMobsButton = Button.builder(
                            Component.literal("Damage Passive Mobs: " + (damagePassiveMobsValue ? "ON" : "OFF")),
                            btn -> {
                                damagePassiveMobsValue = !damagePassiveMobsValue;
                                updatePassiveMobsButton();
                                updateVillagersButton(); // Update villagers button state (greyed out or not)
                            })
                    .bounds(width / 2 - 80, height / 6 + 11, 160, 20)
                    .tooltip(Tooltip.create(Component.literal("Show wounds on passive mobs (animals, etc.)")))
                    .build();
            addRenderableWidget(passiveMobsButton);

            // Toggle for villagers (greyed out if passive mobs is disabled)
            villagersButton = Button.builder(
                            Component.literal("Damage Villagers: " + (damageVillagersValue ? "ON" : "OFF")),
                            btn -> {
                                damageVillagersValue = !damageVillagersValue;
                                updateVillagersButton();
                            })
                    .bounds(width / 2 - 80, height / 6 + 32, 160, 20)
                    .tooltip(Tooltip.create(Component.literal("Show wounds on Villagers and Wandering Traders")))
                    .build();
            addRenderableWidget(villagersButton);

            // Cycle button for damage color
            colorButton = Button.builder(
                            Component.literal("Damage Color: " + damageColorValue.name()),
                            btn -> {
                                // Cycle through colors: RED -> BLACK -> WHITE -> RED
                                damageColorValue = switch (damageColorValue) {
                                    case RED -> ConfigHelper.DamageColor.BLACK;
                                    case BLACK -> ConfigHelper.DamageColor.WHITE;
                                    case WHITE -> ConfigHelper.DamageColor.RED;
                                };
                                updateColorButton();
                            })
                    .bounds(width / 2 - 80, height / 6 + 53, 160, 20)
                    .tooltip(Tooltip.create(Component.literal("Color of wound effects (Red/Black/White)")))
                    .build();
            addRenderableWidget(colorButton);

            // Done button (bottom center, 200x20)
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Done"),
                                    btn -> {
                                        // Apply all values to config
                                        ConfigHelper.INSTANCE.woundsPerTier = woundsPerTierValue;
                                        ConfigHelper.INSTANCE.damagePassiveMobs = damagePassiveMobsValue;
                                        ConfigHelper.INSTANCE.damageVillagers = damageVillagersValue;
                                        ConfigHelper.INSTANCE.damageColor = damageColorValue;

                                        // Save config to disk
                                        ConfigHelper.save();

                                        // Clear texture cache so changes take effect immediately
                                        WoundAssetSelector.cleanup();

                                        // Reload wound texture identifiers with new settings
                                        try {
                                            WoundAssetSelector.loadTextures();
                                        } catch (Exception e) {
                                            VisualHealth.LOGGER.error("Failed to reload wound textures", e);
                                        }

                                        VisualHealth.LOGGER.info("Visual Health config saved: {} wounds per tier, passive mobs: {}, villagers: {}, color: {}",
                                                woundsPerTierValue, damagePassiveMobsValue, damageVillagersValue, damageColorValue);

                                        // Return to parent screen
                                        client.setScreen(parent);
                                    })
                            .bounds(width / 2 - 100, height - 30, 200, 20)
                            .build());

            // Initial update of button states
            updateVillagersButton();
        }

        // Update passive mobs button message
        private void updatePassiveMobsButton() {
            passiveMobsButton.setMessage(Component.literal("Damage Passive Mobs: " + (damagePassiveMobsValue ? "ON" : "OFF")));
        }

        // Update villagers button message and active state
        // Grey out villagers button if passive mobs is disabled
        private void updateVillagersButton() {
            villagersButton.setMessage(Component.literal("Damage Villagers: " + (damageVillagersValue ? "ON" : "OFF")));
            villagersButton.active = damagePassiveMobsValue; // Only active if passive mobs is enabled
        }

        // Update color button message
        private void updateColorButton() {
            colorButton.setMessage(Component.literal("Damage Color: " + damageColorValue.name()));
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            // Draw title at top
            context.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        }
    }
}
