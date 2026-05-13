package win.demistorm.visual_health.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;

import java.util.LinkedHashMap;
import java.util.Map;

// Screen for entity-specific damage color overrides
public class ColorOverridesScreen extends Screen {
    private final Screen parent;
    private final Minecraft client = Minecraft.getInstance();

    private EditBox entityIdInput;
    private OverrideListWidget overrideList;
    private EditBox focusedHexInput = null;
    private String focusedHexEntityId = null;
    private boolean showInvalidText = false;

    private final Map<String, String> overrides;

    private static final String[] MODES = {"RED", "BLACK", "WHITE", "CUSTOM", "EMISSIVE", "DISABLED"};
    private static final int WIDGET_HEIGHT = 20;

    protected ColorOverridesScreen(Screen parent) {
        super(Component.literal("Color Overrides"));
        this.parent = parent;
        this.overrides = new LinkedHashMap<>(ConfigHelper.INSTANCE.colorOverrides);
    }

    @Override
    protected void init() {
        showInvalidText = false;
        int topY = 40;

        entityIdInput = new EditBox(font, 20, topY, 180, WIDGET_HEIGHT, Component.literal("Entity ID"));
        entityIdInput.setHint(Component.literal("e.g. cow or minecraft:cow"));
        entityIdInput.setMaxLength(100);
        addRenderableWidget(entityIdInput);

        addRenderableWidget(
                Button.builder(Component.literal("Add"), btn -> addEntity())
                        .bounds(205, topY, 40, WIDGET_HEIGHT)
                        .build());

        int listTopY = topY + 45;
        int listBottom = height - 45;
        overrideList = new OverrideListWidget(client, width, listTopY, listBottom);
        overrideList.updateEntries();
        addWidget(overrideList);

        addRenderableWidget(
                Button.builder(Component.literal("Done"), btn -> {
                    flushFocusedHex();
                    ConfigHelper.INSTANCE.colorOverrides = new LinkedHashMap<>(overrides);
                    ConfigHelper.save();
                    EntityDamageColors.applyUserOverrides(ConfigHelper.INSTANCE.colorOverrides);
                    ConfigHelper.clearTextureCaches();
                    VisualHealth.LOGGER.info("Color overrides saved: {} entries", overrides.size());
                    client.setScreen(parent);
                }).bounds(width / 2 - 100, height - 30, 200, 20)
                        .build());
    }

    private void addEntity() {
        String text = entityIdInput.getValue().trim();
        if (text.isEmpty()) return;
        String normalized = EntityDamageColors.normalizeEntityId(text);
        if (!EntityDamageColors.isValidEntityId(text)) {
            showInvalidText = true;
            return;
        }
        showInvalidText = false;
        if (!overrides.containsKey(normalized)) {
            overrides.put(normalized, "RED");
            entityIdInput.setValue("");
            overrideList.updateEntries();
        }
    }

    private void removeEntity(String entityId) {
        if (focusedHexInput != null) {
            focusedHexInput.setFocused(false);
        }
        if (entityId.equals(focusedHexEntityId)) {
            focusedHexInput = null;
            focusedHexEntityId = null;
        }
        overrides.remove(entityId);
        overrideList.updateEntries();
    }

    private void cycleMode(String entityId) {
        if (focusedHexInput != null && entityId.equals(focusedHexEntityId)) {
            flushFocusedHex();
            focusedHexInput.setFocused(false);
            focusedHexInput = null;
            focusedHexEntityId = null;
        }

        String current = overrides.getOrDefault(entityId, "RED");
        String baseMode = parseMode(current);
        int idx = indexOf(MODES, baseMode);
        int nextIdx = (idx + 1) % MODES.length;
        String nextMode = MODES[nextIdx];

        String currentHex = nextIdx == 0 ? "FF0000" : parseHex(current);
        if (currentHex == null) currentHex = nextMode.equals("EMISSIVE") ? "FFFFFF" : "FF0000";

        overrides.put(entityId, nextMode.equals("RED") || nextMode.equals("BLACK") || nextMode.equals("WHITE") || nextMode.equals("DISABLED")
                ? nextMode
                : nextMode + ":" + currentHex);
        overrideList.updateEntries();
    }

    private void setHex(String entityId, String hex) {
        String current = overrides.getOrDefault(entityId, "RED");
        String baseMode = parseMode(current);
        if (baseMode.equals("CUSTOM") || baseMode.equals("EMISSIVE")) {
            overrides.put(entityId, baseMode + ":" + hex);
        }
    }

    private void flushFocusedHex() {
        if (focusedHexInput != null && focusedHexEntityId != null) {
            setHex(focusedHexEntityId, focusedHexInput.getValue().trim());
        }
    }

    private static String parseMode(String value) {
        if (value.startsWith("CUSTOM:")) return "CUSTOM";
        if (value.startsWith("EMISSIVE:")) return "EMISSIVE";
        return value;
    }

    private static String parseHex(String value) {
        if (value.startsWith("CUSTOM:")) return value.substring(7);
        if (value.startsWith("EMISSIVE:")) return value.substring(9);
        return null;
    }

    private static int indexOf(String[] arr, String target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(target)) return i;
        }
        return 0;
    }

    @Override
    public void tick() {
        if (focusedHexInput != null && entityIdInput.isFocused()) {
            flushFocusedHex();
            focusedHexInput.setFocused(false);
            focusedHexInput = null;
            focusedHexEntityId = null;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (entityIdInput.isFocused() && (keyCode == 257 || keyCode == 335)) {
            addEntity();
            return true;
        }
        if (focusedHexInput != null) {
            if (focusedHexInput.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedHexInput != null) {
            if (focusedHexInput.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        if (overrideList != null) {
            overrideList.render(context, mouseX, mouseY, delta);
        }

        context.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        context.drawString(font, "Add Entity ID:", 20, 28, 0xFFFFFF);
        if (showInvalidText) {
            int topY = 40;
            context.drawString(font, "Invalid entity!", 250, topY + 6, 0xFF5555);
        }
        context.drawString(font, "Entity Overrides:", 20, overrideList.getTopY() - 15, 0xFFFFFF);
        context.drawString(font, "(" + overrides.size() + " entries)", 140, overrideList.getTopY() - 15, 0xAAAAAA);
    }

    private class OverrideListWidget extends ObjectSelectionList<OverrideListWidget.OverrideEntry> {

        public OverrideListWidget(Minecraft client, int width, int y, int bottom) {
            super(client, width, bottom - y, y, WIDGET_HEIGHT + 4);
        }

        public int getTopY() {
            return getY();
        }

        public void updateEntries() {
            clearEntries();
            for (String entityId : overrides.keySet()) {
                addEntry(new OverrideEntry(entityId));
            }
        }

        @Override
        public int getRowWidth() {
            return width - 20;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.isMouseOver(mouseX, mouseY)) return false;
            for (OverrideEntry entry : this.children()) {
                if (entry.mouseClicked(mouseX, mouseY, button)) return true;
            }
            // Clicked empty space in list, unfocus input
            if (focusedHexInput != null) {
                focusedHexInput.setFocused(false);
                focusedHexInput = null;
                focusedHexEntityId = null;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        public class OverrideEntry extends ObjectSelectionList.Entry<OverrideEntry> {
            private final String entityId;
            private final Button cycleButton;
            private final Button removeButton;
            private final EditBox hexInput;
            private final String mode;
            private final String hexValue;

            OverrideEntry(String entityId) {
                this.entityId = entityId;

                String raw = overrides.getOrDefault(entityId, "RED");
                this.mode = parseMode(raw);
                this.hexValue = parseHex(raw);

                Component tooltip = Component.literal(
                                "RED/BLACK/WHITE: Preset colors\n" +
                                "CUSTOM: Enter a hex color code\n" +
                                "EMISSIVE: Custom color that glows\n" +
                                "DISABLED: No damage rendering");

                this.cycleButton = Button.builder(
                                Component.literal(mode),
                                btn -> cycleMode(entityId))
                        .bounds(0, 0, 70, WIDGET_HEIGHT)
                        .tooltip(Tooltip.create(tooltip))
                        .build();

                this.removeButton = Button.builder(
                                Component.literal(""),
                                btn -> removeEntity(entityId))
                        .bounds(0, 0, 13, 13)
                        .tooltip(Tooltip.create(Component.literal("Remove " + entityId)))
                        .build();

                this.hexInput = new EditBox(font, 0, 0, 80, WIDGET_HEIGHT - 4, Component.literal("Hex"));
                this.hexInput.setHint(Component.literal("FF0000"));
                this.hexInput.setMaxLength(6);
                if (hexValue != null) {
                    this.hexInput.setValue(hexValue);
                }
                this.hexInput.setResponder(value -> {
                    String filtered = value.replaceAll("[^0-9a-fA-F]", "");
                    if (!filtered.equals(value)) {
                        this.hexInput.setValue(filtered);
                    }
                    setHex(entityId, filtered);
                });
            }

            @Override
            public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                boolean showHex = mode.equals("CUSTOM") || mode.equals("EMISSIVE");

                String display = entityId;
                int fixedRightWidth = 70 + 13 + 20;
                if (showHex) fixedRightWidth += 80 + 5;
                int maxLabelWidth = entryWidth - fixedRightWidth;
                if (font.width(display) > maxLabelWidth) {
                    display = font.plainSubstrByWidth(display, maxLabelWidth - 15) + "...";
                }
                context.drawString(font, display, x + 5, y + 6, 0xFFFFFF);

                int removeX = x + entryWidth - 18;
                int removeY = y + (entryHeight - 13) / 2;
                removeButton.setPosition(removeX, removeY);
                removeButton.render(context, mouseX, mouseY, tickDelta);
                context.drawCenteredString(font, "\u00d7", removeX + 7, removeY + 3, 0xFFFFFF);

                int cycleX = removeX - 70 - 5;
                int cycleY = y + (entryHeight - WIDGET_HEIGHT) / 2;
                cycleButton.setPosition(cycleX, cycleY);
                cycleButton.render(context, mouseX, mouseY, tickDelta);

                if (showHex) {
                    int hexX = cycleX - 80 - 5;
                    int hexY = cycleY + 2;
                    hexInput.setPosition(hexX, hexY);
                    hexInput.setWidth(80);
                    hexInput.render(context, mouseX, mouseY, tickDelta);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (cycleButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (removeButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (mode.equals("CUSTOM") || mode.equals("EMISSIVE")) {
                    if (hexInput.mouseClicked(mouseX, mouseY, button)) {
                        if (focusedHexInput != null) {
                            focusedHexInput.setFocused(false);
                        }
                        focusedHexInput = hexInput;
                        focusedHexEntityId = entityId;
                        hexInput.setFocused(true);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Component getNarration() {
                return Component.literal(entityId + " - " + mode);
            }
        }
    }
}
