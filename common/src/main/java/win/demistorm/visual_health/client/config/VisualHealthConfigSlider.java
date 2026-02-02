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
    private final Consumer<Integer> onValueChanged;
    private final Function<Integer, Component> messageFormatter;

    // Create a slider with the given parameters
    // x, y, width, height: Position and size
    // minValue, maxValue: Range of integer values
    // currentValue: Initial value
    // messageFormatter: Function to format the current value into a display message
    // onValueChanged: Callback when value changes
    public VisualHealthConfigSlider(int x, int y, int width, int height,
                                    int minValue, int maxValue, int currentValue,
                                    Function<Integer, Component> messageFormatter,
                                    Consumer<Integer> onValueChanged) {
        super(x, y, width, height, Component.empty(),
              (double)(currentValue - minValue) / (maxValue - minValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.messageFormatter = messageFormatter;
        this.onValueChanged = onValueChanged;
        updateMessage();
    }

    // Update the displayed message with current value
    @Override
    protected void updateMessage() {
        setMessage(messageFormatter.apply(getCurrentIntValue()));
    }

    // Called when user releases the slider handle
    // Applies the new value to the config
    @Override
    protected void applyValue() {
        onValueChanged.accept(getCurrentIntValue());
    }

    // Convert the internal double value to an integer in the correct range
    private int getCurrentIntValue() {
        return Mth.clamp((int)(minValue + (maxValue - minValue) * this.value), minValue, maxValue);
    }

    // Get the current slider value (convenience method)
    public int getValue() {
        return getCurrentIntValue();
    }

    // Set the slider to a specific value (for reset functionality)
    public void setValue(int value) {
        this.value = (double)(value - minValue) / (maxValue - minValue);
        updateMessage();
    }
}
