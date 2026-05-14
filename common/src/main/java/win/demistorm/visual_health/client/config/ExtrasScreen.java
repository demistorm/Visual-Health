package win.demistorm.visual_health.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;

public class ExtrasScreen extends Screen {
    private final Screen parent;
    private final Minecraft client = Minecraft.getInstance();

    private boolean resourcePackEmissivesValue;

    protected ExtrasScreen(Screen parent) {
        super(Component.literal("Extras"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        resourcePackEmissivesValue = ConfigHelper.INSTANCE.drawOnOptifineEmissives;

        addRenderableWidget(
                Button.builder(
                                Component.literal("Resource Pack Emissives: " + (resourcePackEmissivesValue ? "ON" : "OFF")),
                                btn -> {
                                    resourcePackEmissivesValue = !resourcePackEmissivesValue;
                                    btn.setMessage(Component.literal("Resource Pack Emissives: " + (resourcePackEmissivesValue ? "ON" : "OFF")));
                                })
                        .bounds(width / 2 - 80, height / 6, 160, 20)
                        .tooltip(Tooltip.create(Component.literal("Toggles whether to draw damage on resource pack emissive layers (things like glowing eyes, certain accessories)")))
                        .build());

        addRenderableWidget(
                Button.builder(
                                Component.literal("Done"),
                                btn -> {
                                    ConfigHelper.INSTANCE.drawOnOptifineEmissives = resourcePackEmissivesValue;
                                    ConfigHelper.save();
                                    ConfigHelper.clearTextureCaches();
                                    VisualHealth.LOGGER.info("Visual Health extras config saved: resource pack emissives: {}", resourcePackEmissivesValue);
                                    client.setScreen(parent);
                                })
                        .bounds(width / 2 - 100, height - 30, 200, 20)
                        .build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
    }
}
