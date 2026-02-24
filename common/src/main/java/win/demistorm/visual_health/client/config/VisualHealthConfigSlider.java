package win.demistorm.visual_health.client.config;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Function;

public class VisualHealthConfigSlider extends AbstractSliderButton {

    private final int minValue;
    private final int maxValue;
    private final int step;
    private final Consumer<Integer> onValueChanged;
    private final Function<Integer, Component> messageFormatter;

    public VisualHealthConfigSlider(int x, int y, int width, int height,
                                    int minValue, int maxValue, int currentValue,
                                    Function<Integer, Component> messageFormatter,
                                    Consumer<Integer> onValueChanged) {
        this(x, y, width, height, minValue, maxValue, currentValue, 1, messageFormatter, onValueChanged);
    }

    public VisualHealthConfigSlider(int x, int y, int width, int height,
                                    int minValue, int maxValue, int currentValue, int step,
                                    Function<Integer, Component> messageFormatter,
                                    Consumer<Integer> onValueChanged) {
        super(x, y, width, height, Component.empty(),
              (double)(currentValue - minValue) / (maxValue - minValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.messageFormatter = messageFormatter;
        this.onValueChanged = onValueChanged;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(messageFormatter.apply(getCurrentIntValue()));
    }

    @Override
    protected void onDrag(net.minecraft.client.input.MouseButtonEvent mouseButtonEvent, double dragAmountX, double dragAmountY) {
        super.onDrag(mouseButtonEvent, dragAmountX, dragAmountY);

        int snappedValue = getCurrentIntValue();
        this.value = (double)(snappedValue - minValue) / (maxValue - minValue);
        updateMessage();
    }

    @Override
    protected void applyValue() {
        onValueChanged.accept(getCurrentIntValue());
    }

    private int getCurrentIntValue() {
        int rawValue = Mth.clamp((int)(minValue + (maxValue - minValue) * this.value), minValue, maxValue);
        return Math.round(rawValue / (float)step) * step;
    }

    public void setValue(int value) {
        this.value = (double)(value - minValue) / (maxValue - minValue);
        updateMessage();
    }
}
