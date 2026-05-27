package me.anchorhelper.creamykeys.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.anchorhelper.creamykeys.CKSettingsScreen;

public class ModMenuIntegration
implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CKSettingsScreen::new;
    }
}
