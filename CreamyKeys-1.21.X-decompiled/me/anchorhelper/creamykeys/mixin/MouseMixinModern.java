package me.anchorhelper.creamykeys.mixin;

import me.anchorhelper.creamykeys.client.CreamyKeysClient;
import me.anchorhelper.creamykeys.client.KeySoundHandler;
import net.minecraft.class_11910;
import net.minecraft.class_310;
import net.minecraft.class_312;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_312.class})
public abstract class MouseMixinModern {
    @Inject(method={"method_1601(JLnet/minecraft/class_11910;I)V"}, at={@At(value="TAIL")})
    private void creamykeys$onMouseButton(long window, class_11910 input, int action, CallbackInfo ci) {
        int button;
        if (KeySoundHandler.consumeMouseSuppression()) {
            return;
        }
        if (CreamyKeysClient.CONFIG == null || !CreamyKeysClient.CONFIG.enabled) {
            return;
        }
        if (!CreamyKeysClient.CONFIG.mouseSounds) {
            return;
        }
        if (action != 1) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return;
        }
        if (!CreamyKeysClient.CONFIG.playInMenus) {
            if (mc.field_1724 == null) {
                return;
            }
            if (mc.field_1755 != null) {
                return;
            }
        }
        if ((button = input.comp_4801()) == 0 || button == 1) {
            KeySoundHandler.playSecondary();
        }
    }
}
