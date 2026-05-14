package win.demistorm.visual_health.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;

// Config screen for Visual Health
public final class ConfigScreen {

    private ConfigScreen() {
    }

    public static class VisualHealthConfigScreen extends Screen {
        private final Screen parent;
        private final Minecraft client = Minecraft.getInstance();

        private int woundDensityValue = ConfigHelper.INSTANCE.woundDensityPercentage;
        private int damageTierCountValue = ConfigHelper.INSTANCE.damageTierCount;
        private boolean damagePassiveMobsValue = ConfigHelper.INSTANCE.damagePassiveMobs;
        private boolean damageVillagersValue = ConfigHelper.INSTANCE.damageVillagers;
        private ConfigHelper.DamageColor damageColorValue = ConfigHelper.INSTANCE.damageColor;

        private VisualHealthConfigSlider densitySlider;
        private VisualHealthConfigSlider tierSlider;
        private Button passiveMobsButton;
        private Button villagersButton;
        private Button colorButton;

        protected VisualHealthConfigScreen(Screen parent) {
            super(Component.literal("Visual Health Configuration"));
            this.parent = parent;
        }

        public static VisualHealthConfigScreen create(Screen parent) {
            return new VisualHealthConfigScreen(parent);
        }

        @Override
        protected void init() {
            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Reset"),
                                    btn -> {
                                        woundDensityValue = 50;
                                        damageTierCountValue = 5;
                                        damagePassiveMobsValue = true;
                                        damageVillagersValue = false;
                                        damageColorValue = ConfigHelper.DamageColor.RED;

                                        densitySlider.setValue(50);
                                        tierSlider.setValue(5);
                                        updatePassiveMobsButton();
                                        updateVillagersButton();
                                        updateColorButton();
                                    })
                            .bounds(width - 50, 5, 45, 20)
                            .tooltip(Tooltip.create(Component.literal("Reset all settings to default")))
                            .build());

            tierSlider = new VisualHealthConfigSlider(
                    width / 2 - 80,
                    height / 6 - 10,
                    160,
                    20,
                    2,
                    10,
                    damageTierCountValue,
                    1,
                    value -> Component.literal("Damage Tiers: " + value),
                    value -> {
                        damageTierCountValue = value;
                    }
            );
            addRenderableWidget(tierSlider);

            densitySlider = new VisualHealthConfigSlider(
                    width / 2 - 80,
                    height / 6 + 11,
                    160,
                    20,
                    10,
                    100,
                    woundDensityValue,
                    10,
                    value -> Component.literal("Wound Density: " + value + "%"),
                    value -> {
                        woundDensityValue = value;
                    }
            );
            addRenderableWidget(densitySlider);

            passiveMobsButton = Button.builder(
                            Component.literal("Damage Passive Mobs: " + (damagePassiveMobsValue ? "ON" : "OFF")),
                            btn -> {
                                damagePassiveMobsValue = !damagePassiveMobsValue;
                                updatePassiveMobsButton();
                                updateVillagersButton();
                            })
                    .bounds(width / 2 - 80, height / 6 + 32, 160, 20)
                    .tooltip(Tooltip.create(Component.literal("Show wounds on passive mobs (animals, etc.)")))
                    .build();
            addRenderableWidget(passiveMobsButton);

            villagersButton = Button.builder(
                            Component.literal("Damage Villagers: " + (damageVillagersValue ? "ON" : "OFF")),
                            btn -> {
                                damageVillagersValue = !damageVillagersValue;
                                updateVillagersButton();
                            })
                    .bounds(width / 2 - 80, height / 6 + 53, 160, 20)
                    .tooltip(Tooltip.create(Component.literal("Show wounds on Villagers and Wandering Traders")))
                    .build();
            addRenderableWidget(villagersButton);

            colorButton = Button.builder(
                            Component.literal("Damage Color: " + damageColorValue.name()),
                            btn -> {
                                damageColorValue = switch (damageColorValue) {
                                    case RED -> ConfigHelper.DamageColor.BLACK;
                                    case BLACK -> ConfigHelper.DamageColor.WHITE;
                                    case WHITE -> ConfigHelper.DamageColor.RED;
                                };
                                updateColorButton();
                            })
                    .bounds(width / 2 - 80, height / 6 + 74, 160, 20)
                    .tooltip(Tooltip.create(Component.literal("Color of wound effects (Red/Black/White)")))
                    .build();
            addRenderableWidget(colorButton);

            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Entity Overrides..."),
                                    btn -> client.setScreen(new ColorOverridesScreen(this)))
                            .bounds(width / 2 - 80, height / 6 + 95, 160, 20)
                            .tooltip(Tooltip.create(Component.literal("Set or disable custom damage " +
                                    "colors for the player and specific entities!")))
                            .build());

            addRenderableWidget(
                    Button.builder(
                                    Component.literal("Done"),
                                    btn -> {
                                        ConfigHelper.INSTANCE.woundDensityPercentage = woundDensityValue;
                                        ConfigHelper.INSTANCE.damageTierCount = damageTierCountValue;
                                        ConfigHelper.INSTANCE.damagePassiveMobs = damagePassiveMobsValue;
                                        ConfigHelper.INSTANCE.damageVillagers = damageVillagersValue;
                                        ConfigHelper.INSTANCE.damageColor = damageColorValue;

                                        ConfigHelper.save();

                                        EntityDamageColors.applyUserOverrides(ConfigHelper.INSTANCE.colorOverrides);
                                        ConfigHelper.clearTextureCaches();

                                        VisualHealth.LOGGER.info("Visual Health config saved: {}% wound density, {} tiers, passive mobs: {}, villagers: {}, color: {}",
                                                woundDensityValue, damageTierCountValue, damagePassiveMobsValue, damageVillagersValue, damageColorValue);

                                        client.setScreen(parent);
                                    })
                            .bounds(width / 2 - 100, height - 30, 200, 20)
                            .build());

            updateVillagersButton();
        }

        private void updatePassiveMobsButton() {
            passiveMobsButton.setMessage(Component.literal("Damage Passive Mobs: " + (damagePassiveMobsValue ? "ON" : "OFF")));
        }

        private void updateVillagersButton() {
            villagersButton.setMessage(Component.literal("Damage Villagers: " + (damageVillagersValue ? "ON" : "OFF")));
            villagersButton.active = damagePassiveMobsValue;
        }

        private void updateColorButton() {
            colorButton.setMessage(Component.literal("Damage Color: " + damageColorValue.name()));
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
            renderBackground(context);
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        }
    }
}
