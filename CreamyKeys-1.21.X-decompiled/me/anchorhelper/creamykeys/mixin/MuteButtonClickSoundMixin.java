package me.anchorhelper.creamykeys.mixin;

import net.minecraft.class_1144;
import net.minecraft.class_2561;
import net.minecraft.class_339;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_339.class})
public abstract class MuteButtonClickSoundMixin {
    @Shadow
    public abstract class_2561 method_25369();

    @Inject(method={"method_25354(Lnet/minecraft/class_1144;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void creamykeys$muteTestButtons(class_1144 manager, CallbackInfo ci) {
        String s = this.method_25369().getString();
        if (s.equals("Test Keyboard") || s.equals("Test Mouse")) {
            ci.cancel();
        }
    }
}
