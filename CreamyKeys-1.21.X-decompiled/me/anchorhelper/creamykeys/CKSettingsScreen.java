package me.anchorhelper.creamykeys;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import me.anchorhelper.creamykeys.client.CKAudioEngine;
import me.anchorhelper.creamykeys.client.CKKeyboardScanner;
import me.anchorhelper.creamykeys.client.CreamyKeysClient;
import me.anchorhelper.creamykeys.client.KeySoundHandler;
import me.anchorhelper.creamykeys.config.CKConfig;
import me.anchorhelper.creamykeys.update.ModrinthUpdateManager;
import me.anchorhelper.creamykeys.update.UpdateBrowserScreen;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_3532;
import net.minecraft.class_357;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public class CKSettingsScreen
extends class_437 {
    private static final int ROW_GAP = 4;
    private static final int SECTION_GAP = 8;
    private static final int SCROLL_SPEED = 18;
    private static final int MIN_TWO_COL_WIDTH = 360;
    private final class_437 parent;
    private List<String> packs;
    private final List<Row> rows = new ArrayList<Row>();
    private final List<class_339> allWidgets = new ArrayList<class_339>();
    private int contentTop;
    private int contentBottom;
    private int contentHeight;
    private int viewHeight;
    private int maxScroll;
    private int scrollOffset;
    private boolean twoCol;
    private int xLeft;
    private int xRight;
    private int wCol;
    private int xCenter;
    private int wCenter;

    public CKSettingsScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"CreamyKeys"));
        this.parent = parent;
    }

    protected void method_25426() {
        int preservedScroll = this.scrollOffset;
        this.rows.clear();
        this.allWidgets.clear();
        CKKeyboardScanner.scanNow();
        this.packs = CKKeyboardScanner.getPacks();
        CKConfig cfg = CreamyKeysClient.CONFIG;
        CKKeyboardScanner.ensureConfigValid(cfg);
        this.twoCol = this.field_22789 >= 360;
        this.contentTop = Math.max(44, this.field_22790 / 6);
        this.contentBottom = this.field_22790 - 12;
        if (this.contentBottom <= this.contentTop + 20) {
            this.contentTop = 28;
            this.contentBottom = this.field_22790 - 4;
        }
        if (this.contentBottom <= this.contentTop + 20) {
            this.contentTop = 0;
            this.contentBottom = this.field_22790;
        }
        this.computeLayout();
        class_4185 enabled = class_4185.method_46430((class_2561)class_2561.method_43470((String)("Enabled: " + (cfg.enabled ? "ON" : "OFF"))), b -> {
            cfg.enabled = !cfg.enabled;
            cfg.save();
            b.method_25355((class_2561)class_2561.method_43470((String)("Enabled: " + (cfg.enabled ? "ON" : "OFF"))));
        }).method_46434(0, 0, 10, 20).method_46431();
        this.addRowCenter((class_339)enabled, 8);
        class_4185 mouseSelector = class_4185.method_46430((class_2561)class_2561.method_43470((String)("Mouse: " + cfg.keyboardSecondary)), b -> {
            if (this.packs == null || this.packs.isEmpty()) {
                return;
            }
            int idx = Math.max(0, this.packs.indexOf(cfg.keyboardSecondary));
            idx = (idx + 1) % this.packs.size();
            cfg.keyboardSecondary = this.packs.get(idx);
            cfg.save();
            b.method_25355((class_2561)class_2561.method_43470((String)("Mouse: " + cfg.keyboardSecondary)));
            KeySoundHandler.testSecondary();
        }).method_46434(0, 0, 10, 20).method_46431();
        class_4185 keyboardSelector = class_4185.method_46430((class_2561)class_2561.method_43470((String)("Keyboard: " + cfg.keyboardPrimary)), b -> {
            if (this.packs == null || this.packs.isEmpty()) {
                return;
            }
            int idx = Math.max(0, this.packs.indexOf(cfg.keyboardPrimary));
            idx = (idx + 1) % this.packs.size();
            cfg.keyboardPrimary = this.packs.get(idx);
            cfg.save();
            b.method_25355((class_2561)class_2561.method_43470((String)("Keyboard: " + cfg.keyboardPrimary)));
            KeySoundHandler.testPrimary();
        }).method_46434(0, 0, 10, 20).method_46431();
        class_4185 mouseToggle = class_4185.method_46430((class_2561)class_2561.method_43470((String)("Mouse Sounds: " + (cfg.mouseSounds ? "ON" : "OFF"))), b -> {
            cfg.mouseSounds = !cfg.mouseSounds;
            cfg.save();
            b.method_25355((class_2561)class_2561.method_43470((String)("Mouse Sounds: " + (cfg.mouseSounds ? "ON" : "OFF"))));
        }).method_46434(0, 0, 10, 20).method_46431();
        class_4185 keyboardToggle = class_4185.method_46430((class_2561)class_2561.method_43470((String)("Toggle Keyboard: " + (cfg.keyboardSounds ? "ON" : "OFF"))), b -> {
            cfg.keyboardSounds = !cfg.keyboardSounds;
            cfg.save();
            b.method_25355((class_2561)class_2561.method_43470((String)("Toggle Keyboard: " + (cfg.keyboardSounds ? "ON" : "OFF"))));
        }).method_46434(0, 0, 10, 20).method_46431();
        class_4185 testMouse = class_4185.method_46430((class_2561)class_2561.method_43470((String)"Test Mouse"), btn -> KeySoundHandler.testSecondary()).method_46434(0, 0, 10, 20).method_46431();
        class_4185 testKeyboard = class_4185.method_46430((class_2561)class_2561.method_43470((String)"Test Keyboard"), btn -> KeySoundHandler.testPrimary()).method_46434(0, 0, 10, 20).method_46431();
        class_357 mouseVol = this.intSlider(0, 0, 10, Math.round(cfg.volumeMouse * 100.0f), v -> {
            cfg.volumeMouse = Math.max(0.0f, Math.min(1.0f, (float)v.intValue() / 100.0f));
            cfg.save();
        }, val -> class_2561.method_43470((String)("Mouse Volume: " + val + "%")), 0, 100);
        class_357 keyVol = this.intSlider(0, 0, 10, Math.round(cfg.volumeKeyboard * 100.0f), v -> {
            cfg.volumeKeyboard = Math.max(0.0f, Math.min(1.0f, (float)v.intValue() / 100.0f));
            cfg.save();
        }, val -> class_2561.method_43470((String)("Keyboard Volume: " + val + "%")), 0, 100);
        class_357 mouseAmp = this.intSlider(0, 0, 10, cfg.amplifyMouse, v -> {
            cfg.amplifyMouse = Math.max(1, Math.min(16, v));
            cfg.save();
        }, val -> class_2561.method_43470((String)("Mouse Amplify: " + val + "x")), 1, 16);
        class_357 keyAmp = this.intSlider(0, 0, 10, cfg.amplifyKeyboard, v -> {
            cfg.amplifyKeyboard = Math.max(1, Math.min(16, v));
            cfg.save();
        }, val -> class_2561.method_43470((String)("Keyboard Amplify: " + val + "x")), 1, 16);
        if (this.twoCol) {
            this.addRowLR((class_339)mouseSelector, (class_339)keyboardSelector, 4);
            this.addRowLR((class_339)mouseToggle, (class_339)keyboardToggle, 4);
            this.addRowLR((class_339)testMouse, (class_339)testKeyboard, 4);
            this.addRowLR((class_339)mouseVol, (class_339)keyVol, 4);
            this.addRowLR((class_339)mouseAmp, (class_339)keyAmp, 8);
        } else {
            this.addRowCenter((class_339)mouseSelector, 4);
            this.addRowCenter((class_339)keyboardSelector, 4);
            this.addRowCenter((class_339)mouseToggle, 4);
            this.addRowCenter((class_339)keyboardToggle, 4);
            this.addRowCenter((class_339)testMouse, 4);
            this.addRowCenter((class_339)testKeyboard, 4);
            this.addRowCenter((class_339)mouseVol, 4);
            this.addRowCenter((class_339)keyVol, 4);
            this.addRowCenter((class_339)mouseAmp, 4);
            this.addRowCenter((class_339)keyAmp, 8);
        }
        class_4185 openFolder = class_4185.method_46430((class_2561)class_2561.method_43470((String)"Add Sounds"), b -> {
            Path p = CKKeyboardScanner.getKeyboardsDir();
            class_156.method_668().method_672(p.toFile());
        }).method_46434(0, 0, 10, 20).method_46431();
        this.addRowCenter((class_339)openFolder, 4);
        class_4185 rescan = class_4185.method_46430((class_2561)class_2561.method_43470((String)"Rescan Sounds Folder"), b -> {
            CKAudioEngine.clearCache();
            CKKeyboardScanner.scanNow();
            CKKeyboardScanner.ensureConfigValid(cfg);
            this.field_22787.method_1507((class_437)new CKSettingsScreen(this.parent));
        }).method_46434(0, 0, 10, 20).method_46431();
        this.addRowCenter((class_339)rescan, 4);
        class_4185 updates = class_4185.method_46430((class_2561)class_2561.method_43470((String)CKSettingsScreen.updatesLabel()), b -> this.field_22787.method_1507((class_437)new UpdateBrowserScreen(this))).method_46434(0, 0, 10, 20).method_46431();
        this.addRowCenter((class_339)updates, 4);
        class_4185 done = class_4185.method_46430((class_2561)class_2561.method_43470((String)"Done"), b -> this.method_25419()).method_46434(0, 0, 10, 20).method_46431();
        this.addRowCenter((class_339)done, 0);
        this.scrollOffset = preservedScroll;
        this.recalcScrollBounds();
        this.scrollOffset = class_3532.method_15340((int)this.scrollOffset, (int)0, (int)this.maxScroll);
        this.updateWidgetPositions();
    }

    private void computeLayout() {
        int scrollbarReserve = 12;
        int leftPad = 10;
        int rightPad = 10 + scrollbarReserve;
        int gutter = 8;
        int usable = Math.max(0, this.field_22789 - leftPad - rightPad);
        if (this.twoCol) {
            int col = (usable - gutter) / 2;
            if (col < 60) {
                this.twoCol = false;
            } else {
                this.wCol = col;
                this.xLeft = leftPad;
                this.xRight = leftPad + col + gutter;
            }
        }
        int centerMax = Math.min(260, usable);
        int cw = Math.min(220, centerMax);
        this.wCenter = cw = Math.max(60, cw);
        this.xCenter = (this.field_22789 - cw) / 2;
        if (!this.twoCol) {
            this.wCol = 0;
            this.xLeft = 0;
            this.xRight = 0;
        }
    }

    private void addRowCenter(class_339 widget, int gapAfter) {
        Row r = new Row();
        r.center = widget;
        r.gapAfter = gapAfter;
        this.rows.add(r);
        this.allWidgets.add(widget);
        this.method_37063((class_364)widget);
    }

    private void addRowLR(class_339 left, class_339 right, int gapAfter) {
        Row r = new Row();
        r.left = left;
        r.right = right;
        r.gapAfter = gapAfter;
        this.rows.add(r);
        this.allWidgets.add(left);
        this.allWidgets.add(right);
        this.method_37063((class_364)left);
        this.method_37063((class_364)right);
    }

    private void recalcScrollBounds() {
        this.viewHeight = Math.max(0, this.contentBottom - this.contentTop);
        int h = 0;
        for (Row r : this.rows) {
            int rh = 0;
            if (r.center != null) {
                rh = Math.max(rh, r.center.method_25364());
            }
            if (r.left != null) {
                rh = Math.max(rh, r.left.method_25364());
            }
            if (r.right != null) {
                rh = Math.max(rh, r.right.method_25364());
            }
            h += rh + r.gapAfter;
        }
        this.contentHeight = h;
        this.maxScroll = Math.max(0, this.contentHeight - this.viewHeight);
    }

    private void updateWidgetPositions() {
        this.computeLayout();
        this.recalcScrollBounds();
        this.scrollOffset = class_3532.method_15340((int)this.scrollOffset, (int)0, (int)this.maxScroll);
        int baseY = this.contentTop;
        if (this.maxScroll == 0 && this.viewHeight > this.contentHeight) {
            baseY = this.contentTop + (this.viewHeight - this.contentHeight) / 2;
        }
        int yCursor = 0;
        for (Row r : this.rows) {
            int rh = 0;
            if (r.center != null) {
                rh = Math.max(rh, r.center.method_25364());
            }
            if (r.left != null) {
                rh = Math.max(rh, r.left.method_25364());
            }
            if (r.right != null) {
                rh = Math.max(rh, r.right.method_25364());
            }
            int y = baseY + yCursor - this.scrollOffset;
            if (r.center != null) {
                r.center.method_46421(this.xCenter);
                r.center.method_25358(this.wCenter);
                r.center.method_46419(y);
            }
            if (this.twoCol) {
                if (r.left != null) {
                    r.left.method_46421(this.xLeft);
                    r.left.method_25358(this.wCol);
                    r.left.method_46419(y);
                }
                if (r.right != null) {
                    r.right.method_46421(this.xRight);
                    r.right.method_25358(this.wCol);
                    r.right.method_46419(y);
                }
            } else {
                if (r.left != null) {
                    r.left.method_46421(this.xCenter);
                    r.left.method_25358(this.wCenter);
                    r.left.method_46419(y);
                }
                if (r.right != null) {
                    r.right.method_46421(this.xCenter);
                    r.right.method_25358(this.wCenter);
                    r.right.method_46419(y);
                }
            }
            yCursor += rh + r.gapAfter;
        }
    }

    private boolean allowScrollInteraction(double mouseX, double mouseY) {
        class_364 focused = this.method_25399();
        if (focused != null) {
            for (class_339 w : this.allWidgets) {
                if (w != focused) continue;
                return true;
            }
        }
        return mouseY >= (double)this.contentTop && mouseY <= (double)this.contentBottom;
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.allowScrollInteraction(mouseX, mouseY)) {
            return false;
        }
        if (this.maxScroll <= 0) {
            return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int delta = (int)Math.round(-verticalAmount * 18.0);
        if (delta != 0) {
            this.scrollOffset = class_3532.method_15340((int)(this.scrollOffset + delta), (int)0, (int)this.maxScroll);
            this.updateWidgetPositions();
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private class_357 intSlider(int x, int y, int w, int initial, final Consumer<Integer> save, final Function<Integer, class_2561> fmt, final int min, final int max) {
        double init = (double)(initial - min) / (double)(max - min);
        return new class_357(this, x, y, w, 20, fmt.apply(initial), init){

            protected void method_25346() {
                this.method_25355((class_2561)fmt.apply((int)Math.round((double)min + this.field_22753 * (double)(max - min))));
            }

            protected void method_25344() {
                save.accept((int)Math.round((double)min + this.field_22753 * (double)(max - min)));
            }
        };
    }

    public void method_25394(class_332 dc, int mouseX, int mouseY, float delta) {
        this.updateWidgetPositions();
        for (class_339 widget : this.allWidgets) {
            if (widget.method_25369() == null || !widget.method_25369().getString().startsWith("Updates")) continue;
            widget.method_25355((class_2561)class_2561.method_43470((String)CKSettingsScreen.updatesLabel()));
        }
        dc.method_25294(0, 0, this.field_22789, this.field_22790, -1342177280);
        dc.method_44379(0, this.contentTop, this.field_22789, this.contentBottom);
        super.method_25394(dc, mouseX, mouseY, delta);
        dc.method_44380();
        dc.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 20, 0xFFFFFF);
        if (this.maxScroll > 0 && this.viewHeight > 0 && this.contentHeight > 0) {
            int trackX1 = this.field_22789 - 2;
            int trackX0 = this.field_22789 - 5;
            dc.method_25294(trackX0, this.contentTop, trackX1, this.contentBottom, 0x40000000);
            int barH = (int)Math.floor((float)this.viewHeight / (float)this.contentHeight * (float)this.viewHeight);
            barH = Math.max(12, Math.min(this.viewHeight, barH));
            int barRange = this.viewHeight - barH;
            int barY = this.contentTop;
            if (barRange > 0) {
                barY = this.contentTop + Math.round((float)this.scrollOffset / (float)this.maxScroll * (float)barRange);
            }
            dc.method_25294(trackX0, barY, trackX1, barY + barH, -1593835521);
        }
    }

    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }

    private static String updatesLabel() {
        if (ModrinthUpdateManager.isReadyToInstall()) {
            return "Updates (! restart ready)";
        }
        if (ModrinthUpdateManager.isUpdateAvailable()) {
            return "Updates (! available)";
        }
        return "Updates";
    }

    private static final class Row {
        class_339 left;
        class_339 right;
        class_339 center;
        int gapAfter;

        private Row() {
        }
    }
}
