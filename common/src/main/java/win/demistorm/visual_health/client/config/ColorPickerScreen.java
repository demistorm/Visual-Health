package win.demistorm.visual_health.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {

    private static final int SV_SIZE = 150;
    private static final int PREVIEW_SIZE = 40;
    private static final int GAP = 50;
    private static final int WIDGET_HEIGHT = 20;
    private static final int HEX_WIDTH = 90;
    private static final int BASE_Y = 30;
    private static final int LABEL_GAP = 3;
    private static final int SECTION_GAP = 6;

    private final Screen parent;
    private final String initialHex;
    private final Consumer<String> onSave;

    private float hue;
    private float saturation;
    private float brightness;
    private boolean updatingFromPicker = false;

    private SatBrightnessWidget svWidget;
    private HueSliderWidget hueSlider;
    private EditBox hexInput;

    protected ColorPickerScreen(Screen parent, String initialHex, Consumer<String> onSave) {
        super(Component.literal("Pick a Color"));
        this.parent = parent;
        this.initialHex = initialHex;
        this.onSave = onSave;

        int rgb = hexToRgb(initialHex);
        float[] hsv = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.brightness = hsv[2];
    }

    private int getRightColumnWidth() {
        return Math.max(PREVIEW_SIZE, HEX_WIDTH);
    }

    private int getBaseX() {
        int totalWidth = SV_SIZE + GAP + getRightColumnWidth();
        return (width - totalWidth) / 2;
    }

    private int getRightX() {
        return getBaseX() + SV_SIZE + GAP;
    }

    private int getRightColumnStartY() {
        int totalHeight = font.lineHeight + LABEL_GAP + PREVIEW_SIZE + SECTION_GAP
                + WIDGET_HEIGHT + SECTION_GAP
                + font.lineHeight + LABEL_GAP + PREVIEW_SIZE;
        return BASE_Y + (SV_SIZE - totalHeight) / 2;
    }

    @Override
    protected void init() {
        int baseX = getBaseX();
        int rightX = getRightX();
        int rcw = getRightColumnWidth();

        svWidget = new SatBrightnessWidget(baseX, BASE_Y, hue, saturation, brightness,
                (sat, bright) -> {
                    this.saturation = sat;
                    this.brightness = bright;
                    updateHexFromHSV();
                });
        addRenderableWidget(svWidget);

        int hueY = BASE_Y + SV_SIZE + 10;
        hueSlider = new HueSliderWidget(baseX, hueY, hue, h -> {
            this.hue = h;
            svWidget.setHue(h);
            updateHexFromHSV();
        });
        addRenderableWidget(hueSlider);

        int y = getRightColumnStartY();
        y += font.lineHeight + LABEL_GAP;
        y += PREVIEW_SIZE + SECTION_GAP;
        int hexX = rightX + (rcw - HEX_WIDTH) / 2;
        hexInput = new EditBox(font, hexX, y, HEX_WIDTH, WIDGET_HEIGHT, Component.literal("Hex"));
        hexInput.setHint(Component.literal("FF0000"));
        hexInput.setMaxLength(6);
        hexInput.setValue(getHex());
        hexInput.setResponder(value -> {
            if (updatingFromPicker) return;
            String filtered = value.replaceAll("[^0-9a-fA-F]", "");
            if (!filtered.equals(value)) {
                hexInput.setValue(filtered);
                return;
            }
            if (filtered.length() == 6) {
                try {
                    int rgb = Integer.parseInt(filtered, 16);
                    float[] hsv = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
                    this.hue = hsv[0];
                    this.saturation = hsv[1];
                    this.brightness = hsv[2];
                    svWidget.setHue(hue);
                    svWidget.setSaturation(saturation);
                    svWidget.setBrightness(brightness);
                    hueSlider.setValue(hue);
                } catch (NumberFormatException ignored) {
                }
            }
        });
        addRenderableWidget(hexInput);

        addRenderableWidget(
                Button.builder(Component.literal("Save"), btn -> {
                    onSave.accept(getHex());
                    Minecraft.getInstance().gui.setScreen(parent);
                }).bounds(width / 2 - 105, height - 30, 100, WIDGET_HEIGHT)
                        .build());

        addRenderableWidget(
                Button.builder(Component.literal("Cancel"), btn -> Minecraft.getInstance().gui.setScreen(parent))
                        .bounds(width / 2 + 5, height - 30, 100, WIDGET_HEIGHT)
                        .build());
    }

    private void updateHexFromHSV() {
        updatingFromPicker = true;
        hexInput.setValue(getHex());
        updatingFromPicker = false;
    }

    private String getHex() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        return String.format("%06X", rgb & 0x00FFFFFF);
    }

    private static int hexToRgb(String hex) {
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFF0000;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);

        int rightX = getRightX();
        int rcw = getRightColumnWidth();
        int previewX = rightX + (rcw - PREVIEW_SIZE) / 2;

        int y = getRightColumnStartY();
        drawCenteredLabel(graphics, "Previous:", rightX, rcw, y);
        y += font.lineHeight + LABEL_GAP;
        int initialRgb = hexToRgb(initialHex);
        drawColorPreview(graphics, previewX, y, 0xFF000000 | initialRgb);

        y += PREVIEW_SIZE + SECTION_GAP;

        y += WIDGET_HEIGHT + SECTION_GAP;
        drawCenteredLabel(graphics, "Selected:", rightX, rcw, y);
        y += font.lineHeight + LABEL_GAP;
        int currentRgb = Color.HSBtoRGB(hue, saturation, brightness);
        drawColorPreview(graphics, previewX, y, 0xFF000000 | (currentRgb & 0xFFFFFFFF));
    }

    private void drawCenteredLabel(GuiGraphicsExtractor graphics, String text, int rightX, int columnWidth, int y) {
        int textWidth = font.width(text);
        int x = rightX + (columnWidth - textWidth) / 2;
        graphics.text(font, text, x, y, 0xFFAAAAAA);
    }

    private void drawColorPreview(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, color);
        int border = 0xFF404040;
        graphics.fill(x - 1, y - 1, x + PREVIEW_SIZE + 1, y, border);
        graphics.fill(x - 1, y + PREVIEW_SIZE, x + PREVIEW_SIZE + 1, y + PREVIEW_SIZE + 1, border);
        graphics.fill(x - 1, y, x, y + PREVIEW_SIZE, border);
        graphics.fill(x + PREVIEW_SIZE, y, x + PREVIEW_SIZE + 1, y + PREVIEW_SIZE, border);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (hexInput.isFocused() && (keyEvent.key() == 257 || keyEvent.key() == 335)) {
            hexInput.setFocused(false);
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (hexInput.isFocused()) {
            if (hexInput.charTyped(characterEvent)) return true;
        }
        return super.charTyped(characterEvent);
    }
}
