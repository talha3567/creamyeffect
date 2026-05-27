package me.anchorhelper.creamykeys.mixin;

import me.anchorhelper.creamykeys.update.ModrinthUpdateManager;
import me.anchorhelper.creamykeys.update.UpdateBrowserScreen;
import me.anchorhelper.creamykeys.update.UpdateRenderCompat;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_4264;
import net.minecraft.class_437;
import net.minecraft.class_442;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_442.class})
public abstract class TitleScreenMixin
extends class_437 {
    @Unique
    private static final int creamykeys$BADGE_SIZE = 10;
    @Unique
    private static final int creamykeys$PANEL_HEIGHT = 32;
    @Unique
    private static final long creamykeys$PANEL_VISIBLE_MS = 15000L;
    @Unique
    private static final long creamykeys$PANEL_SLIDE_MS = 350L;
    @Unique
    private class_4264 creamykeys$modsButton;
    @Unique
    private class_4185 creamykeys$badgeButton;
    @Unique
    private class_4185 creamykeys$panelButton;
    @Unique
    private int creamykeys$badgeX;
    @Unique
    private int creamykeys$badgeY;
    @Unique
    private int creamykeys$panelX;
    @Unique
    private int creamykeys$panelY;
    @Unique
    private int creamykeys$panelWidth;
    @Unique
    private int creamykeys$panelHeight;
    @Unique
    private boolean creamykeys$badgeVisible;
    @Unique
    private boolean creamykeys$panelVisible;
    @Unique
    private long creamykeys$notificationStartMs;
    @Unique
    private String creamykeys$notificationKey = "";

    protected TitleScreenMixin(class_2561 title) {
        super(title);
    }

    @Inject(method={"method_25426()V"}, at={@At(value="TAIL")})
    private void creamykeys$refreshRects(CallbackInfo ci) {
        this.creamykeys$modsButton = null;
        this.creamykeys$badgeVisible = false;
        this.creamykeys$panelVisible = false;
        this.creamykeys$notificationStartMs = 0L;
        this.creamykeys$notificationKey = "";
        this.creamykeys$badgeButton = (class_4185)this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43473(), button -> this.creamykeys$openUpdater()).method_46434(0, 0, 10, 10).method_46431());
        this.creamykeys$badgeButton.method_25350(0.0f);
        this.creamykeys$badgeButton.field_22764 = false;
        this.creamykeys$badgeButton.field_22763 = false;
        this.creamykeys$panelButton = (class_4185)this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43473(), button -> this.creamykeys$activatePanel()).method_46434(0, 0, 140, 32).method_46431());
        this.creamykeys$panelButton.method_25350(0.0f);
        this.creamykeys$panelButton.field_22764 = false;
        this.creamykeys$panelButton.field_22763 = false;
        this.creamykeys$findModsButton();
        this.creamykeys$syncClickTargets();
    }

    @Inject(method={"method_25394(Lnet/minecraft/class_332;IIF)V"}, at={@At(value="TAIL")})
    private void creamykeys$renderUpdateIndicators(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        String key;
        if (!ModrinthUpdateManager.shouldShowMainMenuNotification()) {
            this.creamykeys$panelVisible = false;
            this.creamykeys$badgeVisible = false;
            this.creamykeys$notificationStartMs = 0L;
            this.creamykeys$notificationKey = "";
            this.creamykeys$syncClickTargets();
            return;
        }
        String string = key = ModrinthUpdateManager.isReadyToInstall() ? "restart" : "update";
        if (!key.equals(this.creamykeys$notificationKey)) {
            this.creamykeys$notificationKey = key;
            this.creamykeys$notificationStartMs = class_156.method_658();
        }
        this.creamykeys$findModsButton();
        this.creamykeys$renderBadge(context, mouseX, mouseY);
        this.creamykeys$renderNotificationPanel(context, mouseX, mouseY);
        this.creamykeys$syncClickTargets();
    }

    @Unique
    private void creamykeys$openUpdater() {
        if (this.field_22787 == null) {
            return;
        }
        this.field_22787.method_1507((class_437)new UpdateBrowserScreen(this));
    }

    @Unique
    private void creamykeys$activatePanel() {
        if (this.field_22787 == null) {
            return;
        }
        if (ModrinthUpdateManager.isReadyToInstall()) {
            ModrinthUpdateManager.restartAndInstall(this.field_22787);
        } else {
            this.field_22787.method_1507((class_437)new UpdateBrowserScreen(this));
        }
    }

    @Unique
    private void creamykeys$syncClickTargets() {
        if (this.creamykeys$badgeButton != null) {
            this.creamykeys$badgeButton.field_22764 = this.creamykeys$badgeVisible;
            this.creamykeys$badgeButton.field_22763 = this.creamykeys$badgeVisible;
            this.creamykeys$badgeButton.method_46421(this.creamykeys$badgeX);
            this.creamykeys$badgeButton.method_46419(this.creamykeys$badgeY);
            this.creamykeys$badgeButton.method_25358(10);
        }
        if (this.creamykeys$panelButton != null) {
            this.creamykeys$panelButton.field_22764 = this.creamykeys$panelVisible;
            this.creamykeys$panelButton.field_22763 = this.creamykeys$panelVisible;
            this.creamykeys$panelButton.method_46421(this.creamykeys$panelX);
            this.creamykeys$panelButton.method_46419(this.creamykeys$panelY);
            this.creamykeys$panelButton.method_25358(Math.max(1, this.creamykeys$panelWidth));
        }
    }

    @Unique
    private void creamykeys$findModsButton() {
        if (this.creamykeys$modsButton != null) {
            return;
        }
        String translatedMods = class_2561.method_43471((String)"modmenu.title").getString();
        for (class_364 child : this.method_25396()) {
            String message;
            class_4264 widget;
            if (!(child instanceof class_4264) || (widget = (class_4264)child) == this.creamykeys$badgeButton || widget == this.creamykeys$panelButton || !(message = widget.method_25369().getString()).equals(translatedMods) && !message.equalsIgnoreCase("Mods")) continue;
            this.creamykeys$modsButton = widget;
            return;
        }
    }

    @Unique
    private void creamykeys$renderBadge(class_332 context, int mouseX, int mouseY) {
        if (this.creamykeys$modsButton == null) {
            this.creamykeys$badgeVisible = false;
            return;
        }
        this.creamykeys$badgeX = this.creamykeys$modsButton.method_46426() + this.creamykeys$modsButton.method_25368() - 10 - 3;
        this.creamykeys$badgeY = this.creamykeys$modsButton.method_46427() + 3;
        this.creamykeys$badgeVisible = true;
        UpdateRenderCompat.drawWarningIcon(context, this.creamykeys$badgeX, this.creamykeys$badgeY, 10);
        if (mouseX >= this.creamykeys$badgeX && mouseX <= this.creamykeys$badgeX + 10 && mouseY >= this.creamykeys$badgeY && mouseY <= this.creamykeys$badgeY + 10) {
            UpdateRenderCompat.drawTooltip(context, this.field_22793, (class_2561)class_2561.method_43470((String)"CreamyKeys update available"), mouseX, mouseY, this.field_22789, this.field_22790);
        }
    }

    @Unique
    private void creamykeys$renderNotificationPanel(class_332 context, int mouseX, int mouseY) {
        long now = class_156.method_658();
        long elapsed = Math.max(0L, now - this.creamykeys$notificationStartMs);
        if (elapsed >= 15350L) {
            this.creamykeys$panelVisible = false;
            return;
        }
        String primary = ModrinthUpdateManager.isReadyToInstall() ? "Update downloaded" : "Update available";
        String secondary = ModrinthUpdateManager.isReadyToInstall() ? "Click to restart" : "Open updater";
        int textWidth = Math.max(this.field_22793.method_1727(primary), this.field_22793.method_1727(secondary));
        int panelWidth = Math.max(140, textWidth + 32);
        int baseX = this.field_22789 - panelWidth - 8;
        int baseY = this.field_22790 - 32 - 8;
        float slideProgress = elapsed <= 15000L ? 0.0f : class_3532.method_15363((float)((float)(elapsed - 15000L) / 350.0f), (float)0.0f, (float)1.0f);
        int slideDistance = panelWidth + 16;
        this.creamykeys$panelWidth = panelWidth;
        this.creamykeys$panelHeight = 32;
        this.creamykeys$panelX = baseX + Math.round((float)slideDistance * slideProgress);
        this.creamykeys$panelY = baseY;
        boolean bl = this.creamykeys$panelVisible = slideProgress < 1.0f;
        if (!this.creamykeys$panelVisible) {
            return;
        }
        boolean hovered = mouseX >= this.creamykeys$panelX && mouseX <= this.creamykeys$panelX + panelWidth && mouseY >= this.creamykeys$panelY && mouseY <= this.creamykeys$panelY + 32;
        int background = hovered ? -266328528 : -300541150;
        int border = ModrinthUpdateManager.isReadyToInstall() ? -4725909 : -800947;
        context.method_25294(this.creamykeys$panelX, this.creamykeys$panelY, this.creamykeys$panelX + panelWidth, this.creamykeys$panelY + 32, background);
        context.method_25294(this.creamykeys$panelX, this.creamykeys$panelY, this.creamykeys$panelX + panelWidth, this.creamykeys$panelY + 1, border);
        context.method_25294(this.creamykeys$panelX, this.creamykeys$panelY + 32 - 1, this.creamykeys$panelX + panelWidth, this.creamykeys$panelY + 32, border);
        context.method_25294(this.creamykeys$panelX, this.creamykeys$panelY, this.creamykeys$panelX + 1, this.creamykeys$panelY + 32, border);
        context.method_25294(this.creamykeys$panelX + panelWidth - 1, this.creamykeys$panelY, this.creamykeys$panelX + panelWidth, this.creamykeys$panelY + 32, border);
        int iconX = this.creamykeys$panelX + 7;
        int iconY = this.creamykeys$panelY + 7;
        UpdateRenderCompat.drawWarningIcon(context, iconX, iconY, 10);
        UpdateRenderCompat.drawTextWithShadow(context, this.field_22793, primary, this.creamykeys$panelX + 20, this.creamykeys$panelY + 6, 0xFFFFFF);
        UpdateRenderCompat.drawTextWithShadow(context, this.field_22793, secondary, this.creamykeys$panelX + 20, this.creamykeys$panelY + 17, 0xD2D2D2);
        int timerX = this.creamykeys$panelX + 1;
        int timerY = this.creamykeys$panelY + 32 - 3;
        int timerWidth = panelWidth - 2;
        context.method_25294(timerX, timerY, timerX + timerWidth, timerY + 2, 0x55000000);
        float timeLeftProgress = class_3532.method_15363((float)(1.0f - (float)elapsed / 15000.0f), (float)0.0f, (float)1.0f);
        int filled = Math.round((float)timerWidth * timeLeftProgress);
        if (filled > 0) {
            context.method_25294(timerX, timerY, timerX + filled, timerY + 2, border);
        }
        if (hovered) {
            String tooltip = ModrinthUpdateManager.isReadyToInstall() ? "CreamyKeys update downloaded" : "CreamyKeys update available";
            UpdateRenderCompat.drawTooltip(context, this.field_22793, (class_2561)class_2561.method_43470((String)tooltip), mouseX, mouseY, this.field_22789, this.field_22790);
        }
    }
}
