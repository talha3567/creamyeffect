package me.anchorhelper.creamykeys;

import java.util.function.Consumer;
import me.anchorhelper.creamykeys.client.KeySoundHandler;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;

public class ToggleButtonWidget
extends class_4185 {
    private final String label;
    private boolean value;
    private final Consumer<Boolean> onChange;

    public ToggleButtonWidget(int x, int y, int w, int h, String label, boolean initial, Consumer<Boolean> onChange) {
        super(x, y, w, h, class_2561.method_30163((String)""), btn -> ((ToggleButtonWidget)btn).handlePress(), field_40754);
        this.label = label;
        this.value = initial;
        this.onChange = onChange;
        this.updateText();
    }

    private void handlePress() {
        this.value = !this.value;
        this.updateText();
        this.onChange.accept(this.value);
        KeySoundHandler.playPrimary();
    }

    private void updateText() {
        this.method_25355((class_2561)class_2561.method_43470((String)(this.label + ": " + (this.value ? "ON" : "OFF"))));
    }

    protected void method_75752(class_332 context, int mouseX, int mouseY, float delta) {
    }
}
