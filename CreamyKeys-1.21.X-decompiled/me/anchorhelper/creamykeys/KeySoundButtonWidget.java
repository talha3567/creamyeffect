package me.anchorhelper.creamykeys;

import me.anchorhelper.creamykeys.client.KeySoundHandler;
import net.minecraft.class_1144;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_744;

public class KeySoundButtonWidget
extends class_4185 {
    public KeySoundButtonWidget(int x, int y, int w, int h, class_2561 msg, class_4185.class_4241 onPress) {
        super(x, y, w, h, msg, b -> {
            KeySoundHandler.suppressNextMouseClick();
            onPress.onPress(b);
        }, field_40754);
    }

    public void method_25354(class_1144 soundManager) {
    }

    public void playDownSound(class_1144 soundManager, class_744 input) {
    }

    protected void method_75752(class_332 context, int mouseX, int mouseY, float delta) {
    }
}
