package win.demistorm.visual_health.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.awt.Color;
import java.util.function.BiConsumer;

public class SatBrightnessWidget extends AbstractWidget {

    private static final int SIZE = 150;
    private static final int CURSOR_SIZE = 5;

    private float hue;
    private float saturation;
    private float brightness;
    private final BiConsumer<Float, Float> onChange;
    private boolean dragging;

    public SatBrightnessWidget(int x, int y, float hue, float saturation, float brightness,
                               BiConsumer<Float, Float> onChange) {
        super(x, y, SIZE, SIZE, Component.literal("Saturation/Brightness"));
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
        this.onChange = onChange;
    }

    public void setHue(float hue) {
        this.hue = hue;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        for (int col = 0; col < SIZE; col++) {
            float sat = (float) col / (SIZE - 1);
            int topColor = Color.HSBtoRGB(hue, sat, 1.0f) | 0xFF000000;
            int bottomColor = Color.HSBtoRGB(hue, sat, 0.0f) | 0xFF000000;
            graphics.fillGradient(getX() + col, getY(), getX() + col + 1, getY() + SIZE, topColor, bottomColor);
        }

        int cx = getX() + Math.round(saturation * (SIZE - 1));
        int cy = getY() + Math.round((1.0f - brightness) * (SIZE - 1));
        int half = CURSOR_SIZE / 2;
        graphics.fill(cx - half - 1, cy - half - 1, cx + half + 2, cy + half + 2, 0xFF000000);
        graphics.fill(cx - half, cy - half, cx + half + 1, cy + half + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (!active || !visible) return false;
        if (!isValidClickButton(mouseButtonEvent.buttonInfo())) return false;
        if (!isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) return false;

        playDownSound(Minecraft.getInstance().getSoundManager());
        updateFromMouse(mouseButtonEvent.x(), mouseButtonEvent.y());
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (!dragging || !isValidClickButton(mouseButtonEvent.buttonInfo())) return false;
        updateFromMouse(mouseButtonEvent.x(), mouseButtonEvent.y());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (dragging && isValidClickButton(mouseButtonEvent.buttonInfo())) {
            dragging = false;
            return true;
        }
        return false;
    }

    private void updateFromMouse(double mouseX, double mouseY) {
        saturation = (float) Mth.clamp((mouseX - getX()) / (SIZE - 1), 0.0, 1.0);
        brightness = (float) Mth.clamp(1.0 - (mouseY - getY()) / (SIZE - 1), 0.0, 1.0);
        onChange.accept(saturation, brightness);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal("Color palette"));
    }
}
