package me.anchorhelper.creamykeys.client;

import me.anchorhelper.creamykeys.client.CKKeyboardScanner;
import me.anchorhelper.creamykeys.config.CKConfig;
import net.fabricmc.api.ClientModInitializer;

public class CreamyKeysClient
implements ClientModInitializer {
    public static CKConfig CONFIG;

    public void onInitializeClient() {
        CONFIG = CKConfig.load();
        CKKeyboardScanner.bootstrap();
        CKKeyboardScanner.scanOnStartup();
        CKKeyboardScanner.ensureConfigValid(CONFIG);
    }
}
