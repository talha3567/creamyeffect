package me.anchorhelper.creamykeys.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import net.fabricmc.loader.api.FabricLoader;

public class CKConfig {
    public static CKConfig INSTANCE = new CKConfig();
    public boolean enabled = true;
    public boolean keyboardSounds = true;
    public boolean mouseSounds = true;
    public boolean playInMenus = true;
    public String keyboardPrimary = "cherrymx_black_pbt";
    public String keyboardSecondary = "cherrymx_blue_abs_2";
    public float volumeKeyboard = 1.0f;
    public float volumeMouse = 1.0f;
    public int amplifyKeyboard = 1;
    public int amplifyMouse = 1;
    public boolean autoUpdate = false;
    public boolean autoUpdateFirstRunComplete = false;
    public boolean autoUpdateTouched = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("creamykeys.json");

    public static CKConfig load() {
        CKConfig c;
        try {
            if (Files.exists(PATH, new LinkOption[0])) {
                String raw = Files.readString(PATH);
                JsonElement el = JsonParser.parseString(raw);
                if (el != null && el.isJsonObject()) {
                    JsonElement v;
                    JsonObject obj = el.getAsJsonObject();
                    CKConfig cfg = GSON.fromJson((JsonElement)obj, CKConfig.class);
                    if (obj.has("allKeys") && !obj.has("playInMenus") && (v = obj.get("allKeys")) != null && v.isJsonPrimitive()) {
                        cfg.playInMenus = v.getAsBoolean();
                    }
                    if (obj.has("mouseKeys") && !obj.has("mouseSounds") && (v = obj.get("mouseKeys")) != null && v.isJsonPrimitive()) {
                        cfg.mouseSounds = v.getAsBoolean();
                    }
                    if (obj.has("volume") && !obj.has("volumeKeyboard") && (v = obj.get("volume")) != null && v.isJsonPrimitive()) {
                        cfg.volumeKeyboard = v.getAsFloat();
                    }
                    if (obj.has("volume") && !obj.has("volumeMouse") && (v = obj.get("volume")) != null && v.isJsonPrimitive()) {
                        cfg.volumeMouse = v.getAsFloat();
                    }
                    if (obj.has("amplify") && !obj.has("amplifyKeyboard") && (v = obj.get("amplify")) != null && v.isJsonPrimitive()) {
                        cfg.amplifyKeyboard = v.getAsInt();
                    }
                    if (obj.has("amplify") && !obj.has("amplifyMouse") && (v = obj.get("amplify")) != null && v.isJsonPrimitive()) {
                        cfg.amplifyMouse = v.getAsInt();
                    }
                    cfg.volumeKeyboard = CKConfig.clamp01(cfg.volumeKeyboard);
                    cfg.volumeMouse = CKConfig.clamp01(cfg.volumeMouse);
                    cfg.amplifyKeyboard = CKConfig.clampInt(cfg.amplifyKeyboard, 1, 16);
                    cfg.amplifyMouse = CKConfig.clampInt(cfg.amplifyMouse, 1, 16);
                    INSTANCE = cfg;
                    cfg.save();
                    return cfg;
                }
                CKConfig fallback = GSON.fromJson(raw, CKConfig.class);
                if (fallback != null) {
                    fallback.volumeKeyboard = CKConfig.clamp01(fallback.volumeKeyboard);
                    fallback.volumeMouse = CKConfig.clamp01(fallback.volumeMouse);
                    fallback.amplifyKeyboard = CKConfig.clampInt(fallback.amplifyKeyboard, 1, 16);
                    fallback.amplifyMouse = CKConfig.clampInt(fallback.amplifyMouse, 1, 16);
                    INSTANCE = fallback;
                    fallback.save();
                    return fallback;
                }
            }
        }
        catch (Exception raw) {
            // empty catch block
        }
        INSTANCE = c = new CKConfig();
        c.save();
        return c;
    }

    public void save() {
        try {
            INSTANCE = this;
            Files.createDirectories(PATH.getParent(), new FileAttribute[0]);
            Files.writeString(PATH, (CharSequence)GSON.toJson(this), new OpenOption[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public static void setAutoUpdate(boolean value) {
        CKConfig.INSTANCE.autoUpdate = value;
        CKConfig.INSTANCE.autoUpdateTouched = true;
        INSTANCE.save();
    }

    public static void completeFirstAutoUpdateRun() {
        if (CKConfig.INSTANCE.autoUpdateFirstRunComplete) {
            return;
        }
        CKConfig.INSTANCE.autoUpdateFirstRunComplete = true;
        if (!CKConfig.INSTANCE.autoUpdateTouched) {
            CKConfig.INSTANCE.autoUpdate = true;
        }
        INSTANCE.save();
    }

    private static float clamp01(float v) {
        if (v < 0.0f) {
            return 0.0f;
        }
        if (v > 1.0f) {
            return 1.0f;
        }
        return v;
    }

    private static int clampInt(int v, int min, int max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }
}
