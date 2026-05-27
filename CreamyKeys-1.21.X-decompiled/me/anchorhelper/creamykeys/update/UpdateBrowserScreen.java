package me.anchorhelper.creamykeys.update;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import me.anchorhelper.creamykeys.update.ModrinthUpdateManager;
import me.anchorhelper.creamykeys.update.UpdateRenderCompat;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_3532;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public class UpdateBrowserScreen
extends class_437 {
    private static final int SCROLL_SPEED = 18;
    private static final int CARD_HEIGHT = 84;
    private static final int CARD_GAP = 8;
    private static final int COLOR_RELEASE_TEXT = -16384998;
    private static final int COLOR_ALPHA_TEXT = -2228146;
    private static final int COLOR_GOLD_BRIGHT = -9894;
    private static final int COLOR_GOLD_NORMAL = -800947;
    private static final int COLOR_INSTALLED = -800947;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());
    private final class_437 parent;
    private final List<Row> rows = new ArrayList<Row>();
    private final List<class_339> rowWidgets = new ArrayList<class_339>();
    private class_4185 refreshButton;
    private class_4185 mostUpToDateButton;
    private class_4185 restartInstallButton;
    private class_4185 doneButton;
    private class_4185 autoUpdateButton;
    private int contentTop;
    private int contentBottom;
    private int contentHeight;
    private int viewHeight;
    private int maxScroll;
    private int scrollOffset;
    private int knownRevision = -1;
    private boolean mostUpToDateMode;
    private boolean refreshOnFirstInit = true;
    private long openedAtMs;
    private float autoUpdateAnim;

    public UpdateBrowserScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"CreamyKeys Updates"));
        this.parent = parent;
    }

    protected void method_25426() {
        if (this.openedAtMs == 0L) {
            this.openedAtMs = System.currentTimeMillis();
        }
        int preservedScroll = this.scrollOffset;
        this.rows.clear();
        this.rowWidgets.clear();
        int headerY = 38;
        int gap = 6;
        int buttonWidth = Math.min(132, (this.field_22789 - 24 - gap * 3) / 4);
        int totalWidth = buttonWidth * 4 + gap * 3;
        int startX = (this.field_22789 - totalWidth) / 2;
        this.refreshButton = (class_4185)this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43470((String)(ModrinthUpdateManager.isChecking() ? "Checking..." : "Refresh")), button -> ModrinthUpdateManager.refresh()).method_46434(startX, headerY, buttonWidth, 20).method_46431());
        this.mostUpToDateButton = (class_4185)this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43473(), button -> {
            this.mostUpToDateMode = !this.mostUpToDateMode;
            this.refreshHeaderButtons();
        }).method_46434(startX + buttonWidth + gap, headerY, buttonWidth, 20).method_46431());
        this.restartInstallButton = (class_4185)this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43470((String)(ModrinthUpdateManager.isReadyToInstall() ? "Restart & Install" : "Restart")), button -> ModrinthUpdateManager.restartAndInstall(class_310.method_1551())).method_46434(startX + (buttonWidth + gap) * 2, headerY, buttonWidth, 20).method_46431());
        this.doneButton = (class_4185)this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43470((String)"Done"), button -> this.method_25419()).method_46434(startX + (buttonWidth + gap) * 3, headerY, buttonWidth, 20).method_46431());
        int autoWidth = Math.min(180, this.field_22789 - 40);
        int autoX = (this.field_22789 - autoWidth) / 2;
        int autoY = headerY + 28;
        this.autoUpdateButton = (class_4185)this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43473(), button -> {
            ModrinthUpdateManager.setAutoUpdateEnabled(!ModrinthUpdateManager.isAutoUpdateEnabled());
            this.refreshHeaderButtons();
        }).method_46434(autoX, autoY, autoWidth, 20).method_46431());
        this.autoUpdateButton.method_25350(0.0f);
        this.autoUpdateAnim = ModrinthUpdateManager.isAutoUpdateEnabled() ? 1.0f : 0.0f;
        this.contentTop = autoY + 28;
        this.contentBottom = this.field_22790 - 34;
        if (this.contentBottom <= this.contentTop + 20) {
            this.contentTop = 88;
            this.contentBottom = this.field_22790 - 6;
        }
        if (this.contentBottom <= this.contentTop + 20) {
            this.contentTop = 0;
            this.contentBottom = this.field_22790;
        }
        if (this.refreshOnFirstInit) {
            this.refreshOnFirstInit = false;
            ModrinthUpdateManager.refresh();
        }
        this.buildRows();
        this.refreshHeaderButtons();
        this.scrollOffset = preservedScroll;
        this.recalcScrollBounds();
        this.scrollOffset = class_3532.method_15340((int)this.scrollOffset, (int)0, (int)this.maxScroll);
        this.updateRowPositions();
        this.knownRevision = ModrinthUpdateManager.getRevision();
    }

    public void method_25393() {
        int currentRevision = ModrinthUpdateManager.getRevision();
        if (currentRevision != this.knownRevision) {
            this.method_41843();
            return;
        }
        float target = ModrinthUpdateManager.isAutoUpdateEnabled() ? 1.0f : 0.0f;
        this.autoUpdateAnim += (target - this.autoUpdateAnim) * 0.24f;
        if (Math.abs(this.autoUpdateAnim - target) < 0.01f) {
            this.autoUpdateAnim = target;
        }
    }

    public void method_25420(class_332 context, int mouseX, int mouseY, float delta) {
    }

    public void method_52752(class_332 context) {
    }

    private void buildRows() {
        for (class_339 widget : this.rowWidgets) {
            this.method_37066((class_364)widget);
        }
        this.rowWidgets.clear();
        this.rows.clear();
        for (ModrinthUpdateManager.RemoteVersion version : ModrinthUpdateManager.getVersions()) {
            class_4185 openButton = class_4185.method_46430((class_2561)class_2561.method_43470((String)"Open Link"), button -> ModrinthUpdateManager.openVersionPage(version)).method_46434(0, 0, 10, 20).method_46431();
            class_4185 downloadButton = class_4185.method_46430((class_2561)class_2561.method_43470((String)"Download"), button -> ModrinthUpdateManager.downloadVersion(version)).method_46434(0, 0, 10, 20).method_46431();
            Row row = new Row(version, openButton, downloadButton);
            this.rows.add(row);
            this.rowWidgets.add((class_339)openButton);
            this.rowWidgets.add((class_339)downloadButton);
            this.method_37063((class_364)openButton);
            this.method_37063((class_364)downloadButton);
        }
    }

    private void refreshHeaderButtons() {
        this.refreshButton.field_22763 = !ModrinthUpdateManager.isChecking() && !ModrinthUpdateManager.isDownloading();
        this.refreshButton.method_25355((class_2561)class_2561.method_43470((String)(ModrinthUpdateManager.isChecking() ? "Checking..." : "Refresh")));
        this.mostUpToDateButton.method_25355((class_2561)class_2561.method_43470((String)(this.mostUpToDateMode ? "Show All" : "Most Up To Date")));
        this.restartInstallButton.field_22763 = ModrinthUpdateManager.isReadyToInstall();
        this.restartInstallButton.method_25355((class_2561)class_2561.method_43470((String)(ModrinthUpdateManager.isReadyToInstall() ? "Restart & Install" : "Restart")));
        if (this.autoUpdateButton != null) {
            this.autoUpdateButton.field_22763 = true;
            this.autoUpdateButton.method_25355((class_2561)class_2561.method_43473());
            this.autoUpdateButton.method_25350(0.0f);
        }
    }

    private void recalcScrollBounds() {
        this.viewHeight = Math.max(0, this.contentBottom - this.contentTop);
        this.contentHeight = this.rows.isEmpty() ? 0 : this.rows.size() * 92 - 8;
        this.maxScroll = Math.max(0, this.contentHeight - this.viewHeight);
    }

    private void updateRowPositions() {
        this.recalcScrollBounds();
        this.scrollOffset = class_3532.method_15340((int)this.scrollOffset, (int)0, (int)this.maxScroll);
        int scrollbarReserve = 12;
        int left = Math.max(12, this.field_22789 / 10);
        int rightPad = 12 + scrollbarReserve;
        int rowWidth = Math.max(220, this.field_22789 - left - rightPad);
        int openWidth = 94;
        int downloadWidth = 94;
        int buttonGap = 8;
        int textWidth = Math.max(120, rowWidth - openWidth - downloadWidth - buttonGap - 26);
        int baseY = this.contentTop;
        if (this.maxScroll == 0 && this.viewHeight > this.contentHeight) {
            baseY = this.contentTop + (this.viewHeight - this.contentHeight) / 2;
        }
        for (int i = 0; i < this.rows.size(); ++i) {
            boolean visible;
            Row row = this.rows.get(i);
            int y = baseY + i * 92 - this.scrollOffset;
            row.x = left;
            row.y = y;
            row.width = rowWidth;
            int openX = left + 12 + textWidth + buttonGap;
            int downloadX = openX + openWidth + buttonGap;
            int buttonY = y + 46;
            row.openButton.method_25358(openWidth);
            row.openButton.method_46421(openX);
            row.openButton.method_46419(buttonY);
            row.downloadButton.method_25358(downloadWidth);
            row.downloadButton.method_46421(downloadX);
            row.downloadButton.method_46419(buttonY);
            row.openButton.field_22764 = visible = y + 84 >= this.contentTop && y <= this.contentBottom;
            row.downloadButton.field_22764 = visible;
            row.openButton.field_22763 = visible && !ModrinthUpdateManager.isDownloading();
            row.downloadButton.field_22763 = visible && !ModrinthUpdateManager.isDownloading() && ModrinthUpdateManager.canSelfInstall();
        }
    }

    private boolean allowScrollInteraction(double mouseX, double mouseY) {
        class_364 focused = this.method_25399();
        if (focused != null) {
            for (class_339 widget : this.rowWidgets) {
                if (widget != focused) continue;
                return true;
            }
        }
        return mouseY >= (double)this.contentTop && mouseY <= (double)this.contentBottom;
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.allowScrollInteraction(mouseX, mouseY)) {
            return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (this.maxScroll <= 0) {
            return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int delta = (int)Math.round(-verticalAmount * 18.0);
        if (delta != 0) {
            this.scrollOffset = class_3532.method_15340((int)(this.scrollOffset + delta), (int)0, (int)this.maxScroll);
            this.updateRowPositions();
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        context.method_25294(0, 0, this.field_22789, this.field_22790, -16644856);
        this.refreshHeaderButtons();
        this.updateRowPositions();
        UpdateRenderCompat.drawCenteredTextWithShadow(context, this.field_22793, this.field_22785.getString(), this.field_22789 / 2, 12, -1);
        UpdateRenderCompat.drawCenteredTextWithShadow(context, this.field_22793, "Installed: " + ModrinthUpdateManager.getInstalledVersionLabel(), this.field_22789 / 2, 24, -800947);
        UpdateRenderCompat.drawCenteredTextWithShadow(context, this.field_22793, ModrinthUpdateManager.getStatusMessage(), this.field_22789 / 2, this.field_22790 - 20, -10401);
        context.method_44379(0, this.contentTop, this.field_22789, this.contentBottom);
        this.drawRows(context);
        context.method_44380();
        super.method_25394(context, mouseX, mouseY, delta);
        this.drawAutoUpdateMask(context);
        if (!ModrinthUpdateManager.canSelfInstall()) {
            UpdateRenderCompat.drawCenteredTextWithShadow(context, this.field_22793, "One-click install only works from a normal installed mod jar.", this.field_22789 / 2, this.contentTop - 16, -22016);
        }
        if (ModrinthUpdateManager.isDownloading()) {
            int barWidth = Math.min(300, this.field_22789 - 40);
            int x = (this.field_22789 - barWidth) / 2;
            int y = this.field_22790 - 34;
            context.method_25294(x, y, x + barWidth, y + 8, -15592942);
            int fill = Math.round((float)barWidth * ModrinthUpdateManager.getDownloadProgress());
            context.method_25294(x, y, x + fill, y + 8, -800947);
        }
        if (this.maxScroll > 0 && this.viewHeight > 0 && this.contentHeight > 0) {
            int trackX1 = this.field_22789 - 2;
            int trackX0 = this.field_22789 - 5;
            context.method_25294(trackX0, this.contentTop, trackX1, this.contentBottom, -15066598);
            int thumbHeight = (int)Math.floor((float)this.viewHeight / (float)this.contentHeight * (float)this.viewHeight);
            thumbHeight = Math.max(12, Math.min(this.viewHeight, thumbHeight));
            int thumbRange = this.viewHeight - thumbHeight;
            int thumbY = this.contentTop;
            if (thumbRange > 0 && this.maxScroll > 0) {
                thumbY = this.contentTop + Math.round((float)this.scrollOffset / (float)this.maxScroll * (float)thumbRange);
            }
            context.method_25294(trackX0, thumbY, trackX1, thumbY + thumbHeight, -1);
        }
    }

    private void drawAutoUpdateMask(class_332 context) {
        if (this.autoUpdateButton == null || !this.autoUpdateButton.field_22764) {
            return;
        }
        int x = this.autoUpdateButton.method_46426();
        int y = this.autoUpdateButton.method_46427();
        int width = this.autoUpdateButton.method_25368();
        int height = this.autoUpdateButton.method_25364();
        boolean hovered = this.autoUpdateButton.method_49606() || this.autoUpdateButton.method_25370();
        boolean on = ModrinthUpdateManager.isAutoUpdateEnabled();
        int bg = hovered ? -266853325 : -300868826;
        int border = hovered ? -800947 : -7443687;
        context.method_25294(x, y, x + width, y + height, bg);
        context.method_25294(x, y, x + width, y + 1, border);
        context.method_25294(x, y + height - 1, x + width, y + height, border);
        context.method_25294(x, y, x + 1, y + height, border);
        context.method_25294(x + width - 1, y, x + width, y + height, border);
        String text = "Auto Update: " + (on ? "On" : "Off");
        int textColor = on ? -2359333 : -3092272;
        Objects.requireNonNull(this.field_22793);
        UpdateRenderCompat.drawTextWithShadow(context, this.field_22793, text, x + 8, y + (height - 9) / 2, textColor);
        int switchW = 28;
        int switchH = 12;
        int switchX = x + width - switchW - 8;
        int switchY = y + (height - switchH) / 2;
        int offTrack = -12892846;
        int onTrack = -16384998;
        int trackColor = UpdateBrowserScreen.lerpColor(offTrack, onTrack, this.autoUpdateAnim);
        context.method_25294(switchX, switchY, switchX + switchW, switchY + switchH, trackColor);
        context.method_25294(switchX + 1, switchY + 1, switchX + switchW - 1, switchY + switchH - 1, 0x22000000);
        int knobW = 10;
        int knobX = switchX + 1 + (int)((float)(switchW - knobW - 2) * this.autoUpdateAnim);
        int knobColor = on ? -1 : -1907998;
        context.method_25294(knobX, switchY + 1, knobX + knobW, switchY + switchH - 1, knobColor);
    }

    private void drawRows(class_332 context) {
        if (this.rows.isEmpty()) {
            String line = ModrinthUpdateManager.isChecking() ? "Checking Modrinth..." : "No compatible versions found.";
            UpdateRenderCompat.drawCenteredTextWithShadow(context, this.field_22793, line, this.field_22789 / 2, this.contentTop + 20, -1);
            return;
        }
        String installedVersion = UpdateBrowserScreen.normalize(ModrinthUpdateManager.getInstalledVersionLabel());
        ModrinthUpdateManager.RemoteVersion latestRelease = ModrinthUpdateManager.getLatestReleaseVersion();
        ModrinthUpdateManager.RemoteVersion downloadedVersion = ModrinthUpdateManager.getDownloadedVersion();
        long elapsed = System.currentTimeMillis() - this.openedAtMs;
        for (Row row : this.rows) {
            int innerBorder;
            boolean dimmedAlpha;
            if (row.y + 84 < this.contentTop || row.y > this.contentBottom) continue;
            boolean release = UpdateBrowserScreen.isReleaseType(row.version.versionType);
            boolean latest = latestRelease != null && latestRelease.id.equals(row.version.id);
            boolean downloaded = downloadedVersion != null && downloadedVersion.id.equals(row.version.id);
            boolean installed = installedVersion.equals(UpdateBrowserScreen.normalize(row.version.versionNumber));
            boolean bl = dimmedAlpha = this.mostUpToDateMode && !release && !installed;
            int outerBorder = installed ? -800947 : (latest ? -9894 : -800947);
            int innerBackground = latest ? -16116703 : -16314342;
            int n = innerBorder = latest ? -15062467 : -15523535;
            if (dimmedAlpha) {
                outerBorder = UpdateBrowserScreen.scaleRgb(outerBorder, 0.1f);
                innerBorder = UpdateBrowserScreen.scaleRgb(innerBorder, 0.1f);
            }
            context.method_25294(row.x, row.y, row.x + row.width, row.y + 84, outerBorder);
            context.method_25294(row.x + 1, row.y + 1, row.x + row.width - 1, row.y + 84 - 1, innerBackground);
            context.method_25294(row.x + 2, row.y + 2, row.x + row.width - 2, row.y + 3, innerBorder);
            context.method_25294(row.x + 2, row.y + 84 - 3, row.x + row.width - 2, row.y + 84 - 2, innerBorder);
            context.method_25294(row.x + 2, row.y + 2, row.x + 3, row.y + 84 - 2, innerBorder);
            context.method_25294(row.x + row.width - 3, row.y + 2, row.x + row.width - 2, row.y + 84 - 2, innerBorder);
            if (release) {
                this.drawFlowBar(context, row.x + 3, row.y + 2, row.width - 6, 2, elapsed, dimmedAlpha ? 0.35f : 1.0f);
                this.drawFlowBar(context, row.x + 3, row.y + 84 - 4, row.width - 6, 2, elapsed, dimmedAlpha ? 0.35f : 1.0f);
            }
            int textX = row.x + 8;
            int versionColor = release ? -16384998 : -2228146;
            UpdateRenderCompat.drawTextWithShadow(context, this.field_22793, row.version.versionNumber + "  [" + row.version.versionType + "]", textX, row.y + 8, versionColor);
            UpdateRenderCompat.drawTextWithShadow(context, this.field_22793, row.version.name, textX, row.y + 22, -2565928);
            UpdateRenderCompat.drawTextWithShadow(context, this.field_22793, "Published: " + DATE_FORMATTER.format(row.version.datePublished), textX, row.y + 36, -4671304);
            UpdateRenderCompat.drawTextWithShadow(context, this.field_22793, UpdateBrowserScreen.shorten(row.version.fileName, 54), textX, row.y + 50, -6316129);
            String badge = downloaded ? "Downloaded" : (installed ? "Installed" : (latest ? "Latest Release" : "Available"));
            int badgeWidth = this.field_22793.method_1727(badge) + 8;
            int badgeX = row.x + row.width - badgeWidth - 10;
            context.method_25294(badgeX, row.y + 8, badgeX + badgeWidth, row.y + 20, -16777216);
            UpdateRenderCompat.drawTextWithShadow(context, this.field_22793, badge, badgeX + 4, row.y + 10, -1);
        }
    }

    private void drawFlowBar(class_332 context, int x, int y, int width, int height, long elapsedMs, float brightness) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int baseColor = UpdateBrowserScreen.scaleRgb(-7707379, brightness);
        context.method_25294(x, y, x + width, y + height, baseColor);
        int stripeWidth = 10;
        int stripeStep = 4;
        float repeats = Math.max(3.0f, (float)width / 64.0f);
        float time = (float)elapsedMs / 700.0f;
        for (int px = 0; px < width; px += stripeStep) {
            float u = (float)px / (float)width;
            float primary = 0.5f + 0.5f * (float)Math.sin((float)Math.PI * 2 * (u * repeats - time));
            float secondary = 0.5f + 0.5f * (float)Math.sin((float)Math.PI * 2 * (u * repeats * 0.5f - time * 0.6f) + 1.2f);
            float glow = class_3532.method_15363((float)(primary * 0.78f + secondary * 0.22f), (float)0.0f, (float)1.0f);
            int color = UpdateBrowserScreen.lerpColor(-4749807, -3918, glow);
            color = UpdateBrowserScreen.scaleRgb(color, brightness);
            int x2 = Math.min(x + width, x + px + stripeWidth);
            context.method_25294(x + px, y, x2, y + height, color);
        }
    }

    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }

    private static String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String normalize(String value) {
        int dashIndex;
        String out = value == null ? "" : value;
        int plusIndex = out.indexOf(43);
        if (plusIndex >= 0) {
            out = out.substring(0, plusIndex);
        }
        if ((dashIndex = out.indexOf(45)) >= 0) {
            out = out.substring(0, dashIndex);
        }
        return out.toLowerCase(Locale.ROOT);
    }

    private static boolean isReleaseType(String versionType) {
        String normalized = versionType == null ? "" : versionType.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || normalized.equals("release") || normalized.equals("stable");
    }

    private static int scaleRgb(int color, float amount) {
        amount = Math.max(0.0f, Math.min(1.0f, amount));
        int a = color >>> 24 & 0xFF;
        int r = (int)((float)(color >>> 16 & 0xFF) * amount);
        int g = (int)((float)(color >>> 8 & 0xFF) * amount);
        int b = (int)((float)(color & 0xFF) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = from >>> 24 & 0xFF;
        int r1 = from >>> 16 & 0xFF;
        int g1 = from >>> 8 & 0xFF;
        int b1 = from & 0xFF;
        int a2 = to >>> 24 & 0xFF;
        int r2 = to >>> 16 & 0xFF;
        int g2 = to >>> 8 & 0xFF;
        int b2 = to & 0xFF;
        int a = (int)((float)a1 + (float)(a2 - a1) * t);
        int r = (int)((float)r1 + (float)(r2 - r1) * t);
        int g = (int)((float)g1 + (float)(g2 - g1) * t);
        int b = (int)((float)b1 + (float)(b2 - b1) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static final class Row {
        private final ModrinthUpdateManager.RemoteVersion version;
        private final class_4185 openButton;
        private final class_4185 downloadButton;
        private int x;
        private int y;
        private int width;

        private Row(ModrinthUpdateManager.RemoteVersion version, class_4185 openButton, class_4185 downloadButton) {
            this.version = version;
            this.openButton = openButton;
            this.downloadButton = downloadButton;
        }
    }
}
