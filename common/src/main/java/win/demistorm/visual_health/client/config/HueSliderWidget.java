package win.demistorm.visual_health.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.awt.Color;
import java.util.function.Consumer;

public class HueSliderWidget extends AbstractSliderButton {

    private static final ResourceLocation HANDLE_SPRITE = ResourceLocation.withDefaultNamespace("widget/slider_handle");
    private static final ResourceLocation HANDLE_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/slider_handle_highlighted");
    private static final int SLIDER_WIDTH = 150;
    private static final int SLIDER_HEIGHT = 20;

    private final Consumer<Float> onHueChange;

    public HueSliderWidget(int x, int y, float initialHue, Consumer<Float> onHueChange) {
        super(x, y, SLIDER_WIDTH, SLIDER_HEIGHT, Component.empty(), Mth.clamp(initialHue, 0.0, 1.0));
        this.onHueChange = onHueChange;
    }

    public float getHue() {
        return (float) value;
    }

    public void setValue(float hue) {
        this.value = Mth.clamp(hue, 0.0, 1.0);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int insetX = 2;
        int insetY = 2;
        int trackWidth = width - insetX * 2;
        int trackHeight = height - insetY * 2;
        for (int i = 0; i < trackWidth; i++) {
            float h = (float) i / (trackWidth - 1);
            int color = Color.HSBtoRGB(h, 1.0f, 1.0f) | 0xFF000000;
            graphics.fill(getX() + insetX + i, getY() + insetY, getX() + insetX + i + 1, getY() + insetY + trackHeight, color);
        }

        int frameColor = 0xFF404040;
        graphics.fill(getX(), getY(), getX() + width, getY() + insetY, frameColor);
        graphics.fill(getX(), getY() + height - insetY, getX() + width, getY() + height, frameColor);
        graphics.fill(getX(), getY(), getX() + insetX, getY() + height, frameColor);
        graphics.fill(getX() + width - insetX, getY(), getX() + width, getY() + height, frameColor);

        int handleX = getX() + (int) (value * (width - 8));
        ResourceLocation handleSprite = isHovered() ? HANDLE_HIGHLIGHTED_SPRITE : HANDLE_SPRITE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, handleSprite, handleX, getY(), 8, getHeight());
    }

    @Override
    protected void updateMessage() {
    }

    @Override
    protected void applyValue() {
        onHueChange.accept((float) value);
    }
}
