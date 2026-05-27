package me.anchorhelper.creamykeys.client;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import me.anchorhelper.creamykeys.client.CKAudioEngine;
import me.anchorhelper.creamykeys.client.CKKeyboardScanner;
import me.anchorhelper.creamykeys.client.CreamyKeysClient;
import net.minecraft.class_310;

public final class KeySoundHandler {
    private static final AtomicBoolean SUPPRESS_MOUSE_ONCE = new AtomicBoolean(false);

    private KeySoundHandler() {
    }

    public static void playPrimary() {
        if (CreamyKeysClient.CONFIG == null) {
            return;
        }
        if (!CreamyKeysClient.CONFIG.enabled) {
            return;
        }
        if (!CreamyKeysClient.CONFIG.keyboardSounds) {
            return;
        }
        KeySoundHandler.playPack(CreamyKeysClient.CONFIG.keyboardPrimary, CreamyKeysClient.CONFIG.volumeKeyboard, CreamyKeysClient.CONFIG.amplifyKeyboard);
    }

    public static void playSecondary() {
        if (CreamyKeysClient.CONFIG == null) {
            return;
        }
        if (!CreamyKeysClient.CONFIG.enabled) {
            return;
        }
        if (!CreamyKeysClient.CONFIG.mouseSounds) {
            return;
        }
        KeySoundHandler.playPack(CreamyKeysClient.CONFIG.keyboardSecondary, CreamyKeysClient.CONFIG.volumeMouse, CreamyKeysClient.CONFIG.amplifyMouse);
    }

    public static void testPrimary() {
        KeySoundHandler.suppressNextMouseClick();
        if (CreamyKeysClient.CONFIG == null) {
            return;
        }
        if (!CreamyKeysClient.CONFIG.enabled) {
            return;
        }
        KeySoundHandler.playPack(CreamyKeysClient.CONFIG.keyboardPrimary, CreamyKeysClient.CONFIG.volumeKeyboard, CreamyKeysClient.CONFIG.amplifyKeyboard);
    }

    public static void testSecondary() {
        KeySoundHandler.suppressNextMouseClick();
        if (CreamyKeysClient.CONFIG == null) {
            return;
        }
        if (!CreamyKeysClient.CONFIG.enabled) {
            return;
        }
        KeySoundHandler.playPack(CreamyKeysClient.CONFIG.keyboardSecondary, CreamyKeysClient.CONFIG.volumeMouse, CreamyKeysClient.CONFIG.amplifyMouse);
    }

    public static void suppressNextMouseClick() {
        SUPPRESS_MOUSE_ONCE.set(true);
    }

    public static boolean consumeMouseSuppression() {
        return SUPPRESS_MOUSE_ONCE.getAndSet(false);
    }

    private static void playPack(String pack, float customVolume, int amplify) {
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return;
        }
        float v = Math.max(0.0f, Math.min(1.0f, customVolume));
        int layers = Math.max(1, Math.min(16, amplify));
        for (int i = 0; i < layers; ++i) {
            Path chosen = CKKeyboardScanner.pickSound(pack);
            if (chosen == null) {
                return;
            }
            CKAudioEngine.play(chosen, v);
        }
    }
}
