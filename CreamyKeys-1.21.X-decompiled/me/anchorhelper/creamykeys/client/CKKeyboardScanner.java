package me.anchorhelper.creamykeys.client;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.anchorhelper.creamykeys.client.CKAudioEngine;
import me.anchorhelper.creamykeys.client.CKBundledKeyboards;
import me.anchorhelper.creamykeys.config.CKConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public final class CKKeyboardScanner {
    private static final String MODID = "creamykeys";
    private static final String DIR_NAME = "creamykeys_keyboards";
    private static final String HOW_TO = "HOW_TO_ADD.txt";
    private static final String INDEX = "index.txt";
    private static final String SINGLE = "single";
    private static final List<String> BUNDLED_PACKS = List.of("cherrymx_blue_abs", "cherrymx_blue_abs_2", "cherrymx_black_abs", "cherrymx_black_pbt", "cherrymx_brown_abs", "cherrymx_brown_pbt", "cherrymx_red_abs");
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);
    private static final AtomicBoolean STARTUP_SCANNED = new AtomicBoolean(false);
    private static volatile Map<String, List<Path>> PACKS = Map.of();
    private static volatile List<String> PACK_NAMES = List.of();

    private CKKeyboardScanner() {
    }

    public static Path getKeyboardsDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(DIR_NAME);
    }

    public static void bootstrap() {
        if (BOOTSTRAPPED.getAndSet(true)) {
            return;
        }
        Path dir = CKKeyboardScanner.getKeyboardsDir();
        try {
            Files.createDirectories(dir, new FileAttribute[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
        CKKeyboardScanner.ensureHowTo(dir);
        CKBundledKeyboards.installIfMissing();
        CKKeyboardScanner.extractBundledIfMissing(dir);
    }

    public static void scanOnStartup() {
        if (STARTUP_SCANNED.getAndSet(true)) {
            return;
        }
        CKKeyboardScanner.scanNow();
    }

    public static void scanNow() {
        CKKeyboardScanner.bootstrap();
        Path dir = CKKeyboardScanner.getKeyboardsDir();
        LinkedHashMap<String, List<Path>> found = new LinkedHashMap<String, List<Path>>();
        List<Path> single = CKKeyboardScanner.loadSinglePack(dir, dir.resolve(INDEX));
        if (!single.isEmpty()) {
            found.put(SINGLE, single);
        }
        try (Stream<Path> s2 = Files.list(dir);){
            List<Path> topDirs = s2.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).filter(p -> {
                String n = p.getFileName().toString();
                return !n.startsWith(".");
            }).sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))).toList();
            for (Path packDir : topDirs) {
                String name = packDir.getFileName().toString();
                List<Path> sounds = CKKeyboardScanner.loadPackFromDir(packDir, packDir.resolve(INDEX), packDir);
                if (sounds.isEmpty()) continue;
                found.put(name, sounds);
            }
        }
        catch (IOException s2) {
            // empty catch block
        }
        ArrayList names = new ArrayList(found.keySet());
        PACKS = Collections.unmodifiableMap(found);
        PACK_NAMES = Collections.unmodifiableList(names);
        List<Path> warm = CKKeyboardScanner.getAllSounds();
        if (!warm.isEmpty()) {
            CKAudioEngine.warmup(warm, 64);
        }
    }

    public static List<String> getPacks() {
        return PACK_NAMES;
    }

    public static void ensureConfigValid(CKConfig cfg) {
        List<String> packs = CKKeyboardScanner.getPacks();
        if (packs.isEmpty()) {
            return;
        }
        if (cfg.keyboardPrimary == null || cfg.keyboardPrimary.isBlank() || !packs.contains(cfg.keyboardPrimary)) {
            cfg.keyboardPrimary = packs.get(0);
            cfg.save();
        }
        if (cfg.keyboardSecondary == null || cfg.keyboardSecondary.isBlank() || !packs.contains(cfg.keyboardSecondary)) {
            cfg.keyboardSecondary = packs.get(0);
            cfg.save();
        }
    }

    public static Path pickSound(String pack) {
        List<Path> list;
        Map<String, List<Path>> m = PACKS;
        if (m.isEmpty()) {
            return null;
        }
        String p = pack;
        if (p == null || p.isBlank()) {
            p = SINGLE;
        }
        if ((list = m.get(p)) == null || list.isEmpty()) {
            list = m.get(SINGLE);
        }
        if (list == null || list.isEmpty()) {
            return null;
        }
        int idx = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(idx);
    }

    public static List<Path> getAllSounds() {
        Map<String, List<Path>> m = PACKS;
        if (m.isEmpty()) {
            return List.of();
        }
        ArrayList<Path> out = new ArrayList<Path>();
        for (List<Path> v : m.values()) {
            out.addAll(v);
        }
        return out;
    }

    private static boolean isOgg(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".ogg");
    }

    private static List<Path> loadSinglePack(Path dir, Path indexFile) {
        List<Path> all = CKKeyboardScanner.listOggFlat(dir);
        if (all.isEmpty()) {
            return List.of();
        }
        ArrayList<Path> ordered = new ArrayList<Path>();
        HashSet<Path> seen = new HashSet<Path>();
        if (Files.exists(indexFile, new LinkOption[0]) && Files.isRegularFile(indexFile, new LinkOption[0])) {
            List<String> lines;
            try {
                lines = Files.readAllLines(indexFile);
            }
            catch (IOException ignored) {
                lines = List.of();
            }
            for (String raw0 : lines) {
                Path parent;
                Path candidate;
                int hash;
                String raw;
                String line;
                if (raw0 == null || (line = (raw = raw0.replace("\ufeff", "")).trim()).isEmpty() || (hash = line.indexOf(35)) == 0 || hash > 0 && (line = line.substring(0, hash).trim()).isEmpty()) continue;
                line = line.replace('\\', '/');
                try {
                    candidate = dir.resolve(line).normalize();
                }
                catch (RuntimeException ignored) {
                    continue;
                }
                if (!candidate.startsWith(dir) || !Files.isRegularFile(candidate, new LinkOption[0]) || !CKKeyboardScanner.isOgg(candidate) || (parent = candidate.getParent()) == null || !parent.equals(dir) || !seen.add(candidate)) continue;
                ordered.add(candidate);
            }
        }
        for (Path p : all) {
            if (!seen.add(p)) continue;
            ordered.add(p);
        }
        return Collections.unmodifiableList(ordered);
    }

    private static List<Path> listOggFlat(Path dir) {
        List<Path> list;
        block8: {
            Stream<Path> s = Files.list(dir);
            try {
                list = s.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(CKKeyboardScanner::isOgg).sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))).collect(Collectors.toList());
                if (s == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (s != null) {
                        try {
                            s.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException ignored) {
                    return List.of();
                }
            }
            s.close();
        }
        return list;
    }

    private static List<Path> loadPackFromDir(Path scanRoot, Path indexFile, Path restrictRoot) {
        List<Path> all = CKKeyboardScanner.listOggRecursive(scanRoot, restrictRoot);
        if (all.isEmpty()) {
            return List.of();
        }
        ArrayList<Path> ordered = new ArrayList<Path>();
        HashSet<Path> seen = new HashSet<Path>();
        if (Files.exists(indexFile, new LinkOption[0]) && Files.isRegularFile(indexFile, new LinkOption[0])) {
            List<String> lines;
            try {
                lines = Files.readAllLines(indexFile);
            }
            catch (IOException ignored) {
                lines = List.of();
            }
            for (String raw0 : lines) {
                Path candidate;
                int hash;
                String raw;
                String line;
                if (raw0 == null || (line = (raw = raw0.replace("\ufeff", "")).trim()).isEmpty() || (hash = line.indexOf(35)) == 0 || hash > 0 && (line = line.substring(0, hash).trim()).isEmpty()) continue;
                line = line.replace('\\', '/');
                try {
                    candidate = restrictRoot.resolve(line).normalize();
                }
                catch (RuntimeException ignored) {
                    continue;
                }
                if (!candidate.startsWith(restrictRoot) || !Files.isRegularFile(candidate, new LinkOption[0]) || !CKKeyboardScanner.isOgg(candidate) || !seen.add(candidate)) continue;
                ordered.add(candidate);
            }
        }
        for (Path p : all) {
            if (!seen.add(p)) continue;
            ordered.add(p);
        }
        return Collections.unmodifiableList(ordered);
    }

    private static List<Path> listOggRecursive(Path root, Path restrictRoot) {
        List<Path> list;
        block8: {
            Stream<Path> w = Files.walk(root, new FileVisitOption[0]);
            try {
                list = w.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(p -> p.startsWith(restrictRoot)).filter(CKKeyboardScanner::isOgg).sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))).collect(Collectors.toList());
                if (w == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (w != null) {
                        try {
                            w.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException ignored) {
                    return List.of();
                }
            }
            w.close();
        }
        return list;
    }

    private static void ensureHowTo(Path dir) {
        Path how = dir.resolve(HOW_TO);
        if (Files.exists(how, new LinkOption[0])) {
            return;
        }
        String txt = "CreamyKeys Keyboards Folder\n\nDrop .ogg files in this folder to create a simple keyboard named 'single'.\nOr create a folder (folder name becomes the keyboard name) and put .ogg files inside.\nFolders can have subfolders.\n\nOptional: add an index.txt inside a keyboard folder to control ordering.\nEach line is a relative path to an .ogg inside that keyboard folder.\nYou can add inline comments with # (anything after # is ignored).\nEven with index.txt, any extra .ogg files are still included.\n\nOpen the CreamyKeys config screen and press Rescan to refresh.\n";
        try {
            Files.writeString(how, (CharSequence)txt, new OpenOption[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static void extractBundledIfMissing(Path dir) {
        Optional modOpt = FabricLoader.getInstance().getModContainer(MODID);
        if (modOpt.isEmpty()) {
            return;
        }
        ModContainer mod = (ModContainer)modOpt.get();
        for (String pack : BUNDLED_PACKS) {
            Path idx;
            Path target = dir.resolve(pack);
            if (CKKeyboardScanner.hasAnyOgg(target)) continue;
            try {
                Files.createDirectories(target, new FileAttribute[0]);
            }
            catch (IOException iOException) {
                // empty catch block
            }
            ArrayList<CallSite> wrote = new ArrayList<CallSite>();
            for (int i = 1; i <= 40; ++i) {
                Object nn = i < 10 ? "0" + i : Integer.toString(i);
                String fn = "key_" + (String)nn + ".ogg";
                String res = "assets/creamykeys/sounds/keyboards/" + pack + "/" + fn;
                Optional src = mod.findPath(res);
                if (src.isEmpty()) {
                    if (i != 1) continue;
                    break;
                }
                try {
                    Files.copy((Path)src.get(), target.resolve(fn), new CopyOption[0]);
                    wrote.add((CallSite)((Object)fn));
                    continue;
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (wrote.isEmpty() || Files.exists(idx = target.resolve(INDEX), new LinkOption[0])) continue;
            try {
                Files.write(idx, wrote, new OpenOption[0]);
            }
            catch (IOException iOException) {}
        }
    }

    private static boolean hasAnyOgg(Path dir) {
        boolean bl;
        block9: {
            if (!Files.isDirectory(dir, new LinkOption[0])) {
                return false;
            }
            Stream<Path> s = Files.walk(dir, new FileVisitOption[0]);
            try {
                bl = s.anyMatch(p -> Files.isRegularFile(p, new LinkOption[0]) && CKKeyboardScanner.isOgg(p));
                if (s == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (s != null) {
                        try {
                            s.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException ignored) {
                    return false;
                }
            }
            s.close();
        }
        return bl;
    }
}
