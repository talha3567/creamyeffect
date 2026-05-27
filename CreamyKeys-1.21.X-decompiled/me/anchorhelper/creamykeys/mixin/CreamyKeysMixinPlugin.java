package me.anchorhelper.creamykeys.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class CreamyKeysMixinPlugin
implements IMixinConfigPlugin {
    private boolean modernInputVersion;

    public void onLoad(String mixinPackage) {
        this.modernInputVersion = CreamyKeysMixinPlugin.isModernInputVersion();
    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("MouseMixinLegacy")) {
            return !this.modernInputVersion;
        }
        if (mixinClassName.endsWith("MouseMixinModern")) {
            return this.modernInputVersion;
        }
        if (mixinClassName.endsWith("KeyboardMixinLegacy")) {
            return !this.modernInputVersion;
        }
        if (mixinClassName.endsWith("KeyboardMixinModern")) {
            return this.modernInputVersion;
        }
        return true;
    }

    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    public List<String> getMixins() {
        return null;
    }

    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isModernInputVersion() {
        String version = FabricLoader.getInstance().getModContainer("minecraft").map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("1.21");
        int[] parts = CreamyKeysMixinPlugin.parseVersion(version);
        int major = parts[0];
        int minor = parts[1];
        int patch = parts[2];
        if (major != 1) {
            return major > 1;
        }
        if (minor != 21) {
            return minor > 21;
        }
        return patch >= 9;
    }

    private static int[] parseVersion(String version) {
        int[] out = new int[]{1, 21, 0};
        if (version == null || version.isEmpty()) {
            return out;
        }
        int index = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < version.length() && index < 3; ++i) {
            char c = version.charAt(i);
            if (Character.isDigit(c)) {
                current.append(c);
                continue;
            }
            if (current.length() <= 0) continue;
            out[index++] = Integer.parseInt(current.toString());
            current.setLength(0);
        }
        if (current.length() > 0 && index < 3) {
            out[index] = Integer.parseInt(current.toString());
        }
        return out;
    }
}
