package me.anchorhelper.creamykeys.mixin;

import me.anchorhelper.creamykeys.client.CreamyKeysClient;
import me.anchorhelper.creamykeys.client.KeySoundHandler;
import net.minecraft.class_309;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_309.class})
public abstract class KeyboardMixinLegacy {
    @Inject(method={"method_1466(JIIII)V"}, at={@At(value="TAIL")}, remap=false, require=0)
    private void creamykeys$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
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
