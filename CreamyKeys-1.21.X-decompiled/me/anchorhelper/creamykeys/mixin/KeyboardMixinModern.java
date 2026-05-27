package me.anchorhelper.creamykeys.mixin;

import me.anchorhelper.creamykeys.client.CreamyKeysClient;
import me.anchorhelper.creamykeys.client.KeySoundHandler;
import net.minecraft.class_11908;
import net.minecraft.class_309;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_309.class})
public abstract class KeyboardMixinModern {
    @Inject(method={"method_1466(JILnet/minecraft/class_11908;)V"}, at={@At(value="TAIL")}, require=0)
    private void creamykeys$onKey(long window, int action, class_11908 input, CallbackInfo ci) {
        if (CreamyKeysClient.CONFIG == null || !CreamyKeysClient.CONFIG.enabled) {
            return;
        }
        if (!CreamyKeysClient.CONFIG.keyboardSounds) {
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
        KeySoundHandler.playPrimary();
    }
}
