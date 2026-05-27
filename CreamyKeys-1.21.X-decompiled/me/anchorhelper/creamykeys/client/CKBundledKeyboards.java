package me.anchorhelper.creamykeys.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Optional;
import java.util.stream.Stream;
import me.anchorhelper.creamykeys.client.CKKeyboardScanner;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public final class CKBundledKeyboards {
    private static final String MODID = "creamykeys";
    private static final String RES_ROOT = "assets/creamykeys/bundled_keyboards";
    private static final String HOW_TO_FILE = "HOW_TO_ADD.txt";

    private CKBundledKeyboards() {
    }

    public static void installIfMissing() {
        Optional modOpt;
        Path root = CKKeyboardScanner.getKeyboardsDir();
        try {
            Files.createDirectories(root, new FileAttribute[0]);
        }
        catch (Exception exception) {
            // empty catch block
        }
        Path how = root.resolve(HOW_TO_FILE);
        if (!Files.exists(how, new LinkOption[0])) {
            try {
                Files.writeString(how, (CharSequence)"Drop .ogg files directly in this folder for a keyboard named 'single'.\nOr create a folder here (folder name becomes the keyboard name) and put .ogg files inside.\nFolders can contain subfolders.\n", StandardCharsets.UTF_8, new OpenOption[0]);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if ((modOpt = FabricLoader.getInstance().getModContainer(MODID)).isEmpty()) {
            return;
        }
        ModContainer mod = (ModContainer)modOpt.get();
        Optional resRootOpt = mod.findPath(RES_ROOT);
        if (resRootOpt.isEmpty()) {
            return;
        }
        Path resRoot = (Path)resRootOpt.get();
        try (Stream<Path> s = Files.walk(resRoot, new FileVisitOption[0]);){
            s.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).forEach(src -> {
                Path rel;
                try {
                    rel = resRoot.relativize((Path)src);
                }
                catch (Exception ignored) {
                    return;
                }
                if (rel == null) {
                    return;
                }
                String relStr = rel.toString();
                if (relStr.isEmpty()) {
                    return;
                }
                Path out = root.resolve(relStr);
                try {
                    Files.createDirectories(out.getParent(), new FileAttribute[0]);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                if (Files.exists(out, new LinkOption[0])) {
                    return;
                }
                try {
                    Files.copy(src, out, new CopyOption[0]);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            });
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}
