package win.demistorm.visual_health.client.config;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Function;

// Custom slider widget for config values
// Extends AbstractSliderButton to provide integer-based slider control
public class VisualHealthConfigSlider extends AbstractSliderButton {

    private final int minValue;
    private final int maxValue;
    private final int step;
    private final Consumer<Integer> onValueChanged;
    private final Function<Integer, Component> messageFormatter;

    // Create a slider with the given parameters
    // x, y, width, height: Position and size
    // minValue, maxValue: Range of integer values
    // currentValue: Initial value
    // step: Step size (e.g., 10 for 10% intervals)
    // messageFormatter: Function to format the current value into a display message
    // onValueChanged: Callback when value changes
    public VisualHealthConfigSlider(int x, int y, int width, int height,
                                    int minValue, int maxValue, int currentValue,
                                    Function<Integer, Component> messageFormatter,
                                    Consumer<Integer> onValueChanged) {
        this(x, y, width, height, minValue, maxValue, currentValue, 1, messageFormatter, onValueChanged);
    }

    // Create a slider with step size support
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

    // Update the displayed message with current value
    @Override
    protected void updateMessage() {
        setMessage(messageFormatter.apply(getCurrentIntValue()));
    }

    // Called when user drags the slider handle
    // Override to make the slider visually snap to step intervals
    @Override
    protected void onDrag(net.minecraft.client.input.MouseButtonEvent mouseButtonEvent, double dragAmountX, double dragAmountY) {
        // Let parent update the value based on mouse position
        super.onDrag(mouseButtonEvent, dragAmountX, dragAmountY);

        // Now snap the visual position to the nearest step interval
        int snappedValue = getCurrentIntValue();
        this.value = (double)(snappedValue - minValue) / (maxValue - minValue);
        updateMessage();
    }

    // Called when user releases the slider handle
    // Applies the new value to the config
    @Override
    protected void applyValue() {
        onValueChanged.accept(getCurrentIntValue());
    }

    // Convert the internal double value to an integer in the correct range
    // Snaps to the nearest step interval for precise control
    private int getCurrentIntValue() {
        int rawValue = Mth.clamp((int)(minValue + (maxValue - minValue) * this.value), minValue, maxValue);
        // Round to nearest step interval
        return Math.round(rawValue / (float)step) * step;
    }

    // Set the slider to a specific value (for reset functionality)
    public void setValue(int value) {
        this.value = (double)(value - minValue) / (maxValue - minValue);
        updateMessage();
    }
}
