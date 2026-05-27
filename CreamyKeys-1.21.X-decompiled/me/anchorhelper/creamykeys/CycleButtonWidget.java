package me.anchorhelper.creamykeys;

import java.util.List;
import java.util.function.Consumer;
import me.anchorhelper.creamykeys.client.KeySoundHandler;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;

public class CycleButtonWidget
extends class_4185 {
    private final String label;
    private final List<String> values;
    private int index;
    private final Consumer<String> onChange;

    public CycleButtonWidget(int x, int y, int w, int h, String label, List<String> values, String initial, Consumer<String> onChange) {
        super(x, y, w, h, class_2561.method_30163((String)""), btn -> ((CycleButtonWidget)btn).handlePress(), field_40754);
        this.label = label;
        this.values = values;
        this.index = Math.max(0, values.indexOf(initial));
        this.onChange = onChange;
        this.updateText();
    }

    private void handlePress() {
        this.index = (this.index + 1) % this.values.size();
        this.updateText();
        this.onChange.accept(this.values.get(this.index));
        KeySoundHandler.playPrimary();
    }

    private void updateText() {
        String v = this.values.get(Math.max(0, this.index));
        this.method_25355((class_2561)class_2561.method_43470((String)(this.label + ": " + v)));
    }

    protected void method_75752(class_332 context, int mouseX, int mouseY, float delta) {
    }
}
