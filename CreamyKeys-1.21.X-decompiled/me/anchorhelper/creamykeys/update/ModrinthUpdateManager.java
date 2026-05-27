package me.anchorhelper.creamykeys.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.anchorhelper.creamykeys.config.CKConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.class_156;
import net.minecraft.class_310;

public final class ModrinthUpdateManager {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "creamykeys-updater");
        thread.setDaemon(true);
        return thread;
    });
    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private static final String API_BASE = "https://api.modrinth.com/v2";
    private static final String PROJECT_SLUG = "creamykeys";
    private static final String PROJECT_PAGE = "https://modrinth.com/mod/creamykeys";
    private static final String PROJECT_VERSIONS_PAGE = "https://modrinth.com/mod/creamykeys/versions";
    private static final String USER_AGENT = "CreamyKeysUpdater/1.0.3 (creamykeys)";
    private static final Path UPDATE_DIR = FabricLoader.getInstance().getConfigDir().resolve("creamykeys_updater");
    private static final Path INSTALL_LOG = UPDATE_DIR.resolve("install.log");
    private static volatile boolean initialized;
    private static volatile boolean checking;
    private static volatile boolean downloading;
    private static volatile boolean sessionAutoUpdateEnabled;
    private static volatile int revision;
    private static volatile String errorMessage;
    private static volatile String statusMessage;
    private static volatile List<RemoteVersion> versions;
    private static volatile RemoteVersion recommendedVersion;
    private static volatile Path downloadedFile;
    private static volatile RemoteVersion downloadedVersion;
    private static volatile long downloadedBytes;
    private static volatile long downloadTotalBytes;

    private ModrinthUpdateManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        sessionAutoUpdateEnabled = CKConfig.INSTANCE.autoUpdateFirstRunComplete && CKConfig.INSTANCE.autoUpdate;
        CKConfig.completeFirstAutoUpdateRun();
        ModrinthUpdateManager.refresh();
    }

    public static void refresh() {
        if (checking || downloading) {
            return;
        }
        checking = true;
        errorMessage = "";
        statusMessage = "Checking for updates...";
        versions = List.of();
        recommendedVersion = null;
        ModrinthUpdateManager.bumpRevision();
        EXECUTOR.execute(() -> {
            try {
                Files.createDirectories(UPDATE_DIR, new FileAttribute[0]);
                List<RemoteVersion> fetchedVersions = ModrinthUpdateManager.fetchVersions();
                RemoteVersion latestRelease = ModrinthUpdateManager.findLatestRelease(fetchedVersions);
                RemoteVersion fallbackRecommended = latestRelease != null ? latestRelease : ModrinthUpdateManager.fetchFallbackRecommendedVersion(fetchedVersions);
                versions = List.copyOf(fetchedVersions);
                recommendedVersion = fallbackRecommended;
                statusMessage = fetchedVersions.isEmpty() ? "No versions were returned by Modrinth." : "Found " + fetchedVersions.size() + " compatible versions.";
                ModrinthUpdateManager.maybeQueueAutoUpdateDownload();
            }
            catch (Exception exception) {
                versions = List.of();
                recommendedVersion = null;
                errorMessage = ModrinthUpdateManager.shortMessage(exception);
                statusMessage = "Update check failed.";
            }
            finally {
                checking = false;
                ModrinthUpdateManager.bumpRevision();
            }
        });
    }

    public static boolean isChecking() {
        return checking;
    }

    public static boolean isDownloading() {
        return downloading;
    }

    public static boolean isUpdateAvailable() {
        return recommendedVersion != null && ModrinthUpdateManager.isVersionNewerThanInstalled(recommendedVersion);
    }

    public static boolean isReadyToInstall() {
        return downloadedFile != null && downloadedVersion != null && Files.exists(downloadedFile, new LinkOption[0]) && ModrinthUpdateManager.getReplacementTarget() != null;
    }

    public static boolean canSelfInstall() {
        return ModrinthUpdateManager.getReplacementTarget() != null;
    }

    public static boolean isAutoUpdateEnabled() {
        return sessionAutoUpdateEnabled;
    }

    public static void setAutoUpdateEnabled(boolean enabled) {
        sessionAutoUpdateEnabled = enabled;
        CKConfig.setAutoUpdate(enabled);
        if (enabled) {
            ModrinthUpdateManager.maybeQueueAutoUpdateDownload();
        }
        ModrinthUpdateManager.bumpRevision();
    }

    public static List<RemoteVersion> getVersions() {
        return versions;
    }

    public static RemoteVersion getRecommendedVersion() {
        return recommendedVersion;
    }

    public static RemoteVersion getLatestReleaseVersion() {
        return ModrinthUpdateManager.findLatestRelease(versions);
    }

    public static RemoteVersion getDownloadedVersion() {
        return downloadedVersion;
    }

    public static String getProjectVersionsPage() {
        return PROJECT_VERSIONS_PAGE;
    }

    public static String getInstalledVersionLabel() {
        return ModrinthUpdateManager.getInstalledVersionRaw();
    }

    public static String getStatusMessage() {
        if (downloading) {
            if (downloadTotalBytes > 0L) {
                return "Downloading " + downloadedBytes / 1024L + " KB / " + downloadTotalBytes / 1024L + " KB";
            }
            return "Downloading update...";
        }
        if (!errorMessage.isEmpty()) {
            return errorMessage;
        }
        if (ModrinthUpdateManager.isReadyToInstall()) {
            return "Downloaded " + ModrinthUpdateManager.downloadedVersion.versionNumber + ". Restart to install it.";
        }
        return statusMessage;
    }

    public static String getTitleScreenMessage() {
        if (ModrinthUpdateManager.isReadyToInstall() && downloadedVersion != null) {
            return "CreamyKeys " + ModrinthUpdateManager.downloadedVersion.versionNumber + " is downloaded. Restart to install it.";
        }
        if (ModrinthUpdateManager.isUpdateAvailable() && recommendedVersion != null) {
            return "CreamyKeys " + ModrinthUpdateManager.recommendedVersion.versionNumber + " is available. Updating is recommended for fixes and stability.";
        }
        if (!errorMessage.isEmpty()) {
            return "CreamyKeys update check failed.";
        }
        return "";
    }

    public static boolean shouldShowMainMenuNotification() {
        return ModrinthUpdateManager.isUpdateAvailable() || ModrinthUpdateManager.isReadyToInstall();
    }

    public static String getNotificationPrimary() {
        if (ModrinthUpdateManager.isReadyToInstall() && downloadedVersion != null) {
            return "CreamyKeys " + ModrinthUpdateManager.downloadedVersion.versionNumber + " is downloaded.";
        }
        if (ModrinthUpdateManager.isUpdateAvailable() && recommendedVersion != null) {
            return "CreamyKeys " + ModrinthUpdateManager.recommendedVersion.versionNumber + " is available.";
        }
        return "";
    }

    public static String getNotificationSecondary() {
        if (ModrinthUpdateManager.isReadyToInstall()) {
            return "Restart is required to install it.";
        }
        if (ModrinthUpdateManager.isUpdateAvailable()) {
            return "Updating is recommended for fixes and stability.";
        }
        return "";
    }

    public static int getRevision() {
        return revision;
    }

    public static float getDownloadProgress() {
        if (!downloading || downloadTotalBytes <= 0L) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, (float)downloadedBytes / (float)downloadTotalBytes));
    }

    public static void openProjectVersionsPage() {
        ModrinthUpdateManager.openUrl(PROJECT_VERSIONS_PAGE);
    }

    public static void openVersionPage(RemoteVersion version) {
        if (version == null) {
            return;
        }
        ModrinthUpdateManager.openUrl(version.versionPageUrl);
    }

    public static void downloadVersion(RemoteVersion version) {
        if (version == null || downloading) {
            return;
        }
        if (!ModrinthUpdateManager.canSelfInstall()) {
            errorMessage = "One-click install only works from a normal jar install.";
            ModrinthUpdateManager.bumpRevision();
            return;
        }
        downloading = true;
        downloadedBytes = 0L;
        downloadTotalBytes = Math.max(0L, version.fileSize);
        errorMessage = "";
        statusMessage = "Downloading " + version.versionNumber + "...";
        ModrinthUpdateManager.bumpRevision();
        EXECUTOR.execute(() -> {
            Path tempFile = null;
            try {
                Files.createDirectories(UPDATE_DIR, new FileAttribute[0]);
                tempFile = UPDATE_DIR.resolve(version.fileName + ".part");
                Path finalFile = UPDATE_DIR.resolve(version.fileName);
                HttpRequest request = HttpRequest.newBuilder(URI.create(version.fileUrl)).timeout(Duration.ofMinutes(2L)).header("User-Agent", USER_AGENT).GET().build();
                HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("Download failed with HTTP " + response.statusCode());
                }
                String contentLength = response.headers().firstValue("Content-Length").orElse(null);
                if (contentLength != null && !contentLength.isBlank()) {
                    try {
                        downloadTotalBytes = Long.parseLong(contentLength);
                    }
                    catch (NumberFormatException numberFormatException) {
                        // empty catch block
                    }
                }
                try (InputStream inputStream = response.body();
                     OutputStream outputStream = Files.newOutputStream(tempFile, new OpenOption[0]);){
                    int read;
                    byte[] buffer = new byte[8192];
                    while ((read = inputStream.read(buffer)) >= 0) {
                        outputStream.write(buffer, 0, read);
                        downloadedBytes += (long)read;
                    }
                }
                Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
                downloadedFile = finalFile;
                downloadedVersion = version;
                statusMessage = "Downloaded " + version.versionNumber + ". Restart to install it.";
            }
            catch (Exception exception) {
                if (tempFile != null) {
                    try {
                        Files.deleteIfExists(tempFile);
                    }
                    catch (IOException iOException) {
                        // empty catch block
                    }
                }
                downloadedFile = null;
                downloadedVersion = null;
                errorMessage = ModrinthUpdateManager.shortMessage(exception);
                statusMessage = "Download failed.";
            }
            finally {
                downloading = false;
                downloadedBytes = Math.max(downloadedBytes, 0L);
                ModrinthUpdateManager.bumpRevision();
            }
        });
    }

    public static void restartAndInstall(class_310 client) {
        if (client == null || !ModrinthUpdateManager.isReadyToInstall()) {
            return;
        }
        Path source = downloadedFile;
        Path target = ModrinthUpdateManager.getReplacementTarget();
        if (source == null || target == null) {
            errorMessage = "Could not find the installed mod jar to replace.";
            ModrinthUpdateManager.bumpRevision();
            return;
        }
        try {
            Files.createDirectories(UPDATE_DIR, new FileAttribute[0]);
            Files.writeString(INSTALL_LOG, (CharSequence)"", StandardCharsets.UTF_8, new OpenOption[0]);
            ModrinthUpdateManager.appendInstallLog("Preparing install");
            ModrinthUpdateManager.appendInstallLog("Selected version: " + ModrinthUpdateManager.downloadedVersion.versionNumber);
            ModrinthUpdateManager.appendInstallLog("Source: " + String.valueOf(source));
            ModrinthUpdateManager.appendInstallLog("Target: " + String.valueOf(target));
            long pid = ProcessHandle.current().pid();
            Path script = ModrinthUpdateManager.writeInstallerScript(source, target, pid, ModrinthUpdateManager.downloadedVersion.versionNumber);
            ModrinthUpdateManager.launchInstaller(script);
            statusMessage = "Installer launched for " + ModrinthUpdateManager.downloadedVersion.versionNumber + ". Closing Minecraft...";
            errorMessage = "";
            ModrinthUpdateManager.bumpRevision();
            client.method_1592();
        }
        catch (Exception exception) {
            errorMessage = ModrinthUpdateManager.shortMessage(exception);
            ModrinthUpdateManager.appendInstallLog("Launch failed: " + errorMessage);
            ModrinthUpdateManager.bumpRevision();
        }
    }

    private static void maybeQueueAutoUpdateDownload() {
        if (!sessionAutoUpdateEnabled || downloading || ModrinthUpdateManager.isReadyToInstall()) {
            return;
        }
        RemoteVersion latestRelease = ModrinthUpdateManager.getLatestReleaseVersion();
        if (latestRelease == null) {
            return;
        }
        if (!ModrinthUpdateManager.isVersionNewerThanInstalled(latestRelease)) {
            return;
        }
        if (downloadedVersion != null && ModrinthUpdateManager.downloadedVersion.id.equals(latestRelease.id) && downloadedFile != null && Files.exists(downloadedFile, new LinkOption[0])) {
            return;
        }
        ModrinthUpdateManager.downloadVersion(latestRelease);
    }

    private static List<RemoteVersion> fetchVersions() throws Exception {
        List<String> requestedGameVersions = ModrinthUpdateManager.getRequestedGameVersions();
        String url = "https://api.modrinth.com/v2/project/creamykeys/version?loaders=" + ModrinthUpdateManager.encodeJsonArray(List.of("fabric")) + "&game_versions=" + ModrinthUpdateManager.encodeJsonArray(requestedGameVersions) + "&include_changelog=false";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15L)).header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Version list request failed with HTTP " + response.statusCode());
        }
        JsonArray root = GSON.fromJson(response.body(), JsonArray.class);
        LinkedHashMap<String, RemoteVersion> parsed = new LinkedHashMap<String, RemoteVersion>();
        if (root != null) {
            for (JsonElement element : root) {
                RemoteVersion version2;
                if (element == null || !element.isJsonObject() || (version2 = ModrinthUpdateManager.parseVersion(element.getAsJsonObject())) == null) continue;
                parsed.putIfAbsent(version2.id, version2);
            }
        }
        String currentGameVersion = ModrinthUpdateManager.getCurrentGameVersion();
        ArrayList<RemoteVersion> out = new ArrayList<RemoteVersion>();
        for (RemoteVersion version3 : parsed.values()) {
            if (!ModrinthUpdateManager.isCompatibleWithCurrentGameVersion(version3, currentGameVersion)) continue;
            out.add(version3);
        }
        out.sort(Comparator.comparing(version -> version.datePublished).reversed());
        return out;
    }

    private static RemoteVersion fetchFallbackRecommendedVersion(List<RemoteVersion> fetchedVersions) throws Exception {
        Path currentJar = ModrinthUpdateManager.getReplacementTarget();
        if (currentJar != null && Files.exists(currentJar, new LinkOption[0])) {
            JsonObject jsonObject;
            RemoteVersion recommended;
            String sha1 = ModrinthUpdateManager.sha1(currentJar);
            String url = "https://api.modrinth.com/v2/version_file/" + sha1 + "/update?algorithm=sha1";
            JsonObject body = new JsonObject();
            body.add("loaders", ModrinthUpdateManager.toJsonArray(List.of("fabric")));
            body.add("game_versions", ModrinthUpdateManager.toJsonArray(ModrinthUpdateManager.getRequestedGameVersions()));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15L)).header("User-Agent", USER_AGENT).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200 && (recommended = ModrinthUpdateManager.parseVersion(jsonObject = GSON.fromJson(response.body(), JsonObject.class))) != null) {
                for (RemoteVersion version : fetchedVersions) {
                    if (!version.id.equals(recommended.id)) continue;
                    return version;
                }
                return recommended;
            }
            if (response.statusCode() != 404) {
                throw new IOException("Recommended update request failed with HTTP " + response.statusCode());
            }
            return null;
        }
        for (RemoteVersion version : fetchedVersions) {
            if (!ModrinthUpdateManager.isVersionNewerThanInstalled(version)) continue;
            return version;
        }
        return null;
    }

    private static RemoteVersion findLatestRelease(List<RemoteVersion> fetchedVersions) {
        for (RemoteVersion version : fetchedVersions) {
            if (!ModrinthUpdateManager.isReleaseType(version.versionType)) continue;
            return version;
        }
        return null;
    }

    private static RemoteVersion parseVersion(JsonObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        JsonArray filesArray = ModrinthUpdateManager.getArray(jsonObject, "files");
        if (filesArray == null || filesArray.size() == 0) {
            return null;
        }
        JsonObject selectedFile = null;
        for (JsonElement fileElement : filesArray) {
            if (fileElement == null || !fileElement.isJsonObject()) continue;
            JsonObject fileObject = fileElement.getAsJsonObject();
            if (ModrinthUpdateManager.getBoolean(fileObject, "primary")) {
                selectedFile = fileObject;
                break;
            }
            if (selectedFile != null) continue;
            selectedFile = fileObject;
        }
        if (selectedFile == null) {
            return null;
        }
        JsonObject hashes = ModrinthUpdateManager.getObject(selectedFile, "hashes");
        String versionNumber = ModrinthUpdateManager.getString(jsonObject, "version_number", "unknown");
        String remoteSha1 = hashes == null ? "" : ModrinthUpdateManager.getString(hashes, "sha1", "");
        List<String> gameVersions = ModrinthUpdateManager.getStringList(jsonObject, "game_versions");
        return new RemoteVersion(ModrinthUpdateManager.getString(jsonObject, "id", versionNumber), ModrinthUpdateManager.getString(jsonObject, "name", versionNumber), versionNumber, ModrinthUpdateManager.getString(jsonObject, "version_type", "release"), ModrinthUpdateManager.parseInstant(ModrinthUpdateManager.getString(jsonObject, "date_published", "1970-01-01T00:00:00Z")), ModrinthUpdateManager.getString(selectedFile, "url", ""), ModrinthUpdateManager.getString(selectedFile, "filename", versionNumber + ".jar"), ModrinthUpdateManager.getString(jsonObject, "changelog", ""), ModrinthUpdateManager.getLong(selectedFile, "size", 0L), remoteSha1, "https://modrinth.com/mod/creamykeys/version/" + ModrinthUpdateManager.getString(jsonObject, "id", versionNumber), gameVersions);
    }

    private static boolean isInstalledVersion(RemoteVersion version) {
        String installedVersion;
        if (version == null) {
            return false;
        }
        Path currentJar = ModrinthUpdateManager.getReplacementTarget();
        if (currentJar != null) {
            try {
                String currentSha1 = ModrinthUpdateManager.sha1(currentJar);
                if (!currentSha1.isBlank() && currentSha1.equalsIgnoreCase(version.sha1)) {
                    return true;
                }
            }
            catch (Exception currentSha1) {
                // empty catch block
            }
        }
        if (ModrinthUpdateManager.normalizeVersionForComparison(installedVersion = ModrinthUpdateManager.getInstalledVersionRaw()).equals(ModrinthUpdateManager.normalizeVersionForComparison(version.versionNumber))) {
            return true;
        }
        return ModrinthUpdateManager.compareVersionOrder(installedVersion, version.versionNumber) == 0;
    }

    private static boolean isVersionNewerThanInstalled(RemoteVersion version) {
        if (version == null) {
            return false;
        }
        if (ModrinthUpdateManager.isInstalledVersion(version)) {
            return false;
        }
        return ModrinthUpdateManager.compareVersionOrder(ModrinthUpdateManager.getInstalledVersionRaw(), version.versionNumber) < 0;
    }

    private static int compareVersionOrder(String installed, String remote) {
        ComparableVersion left = ComparableVersion.parse(installed);
        ComparableVersion right = ComparableVersion.parse(remote);
        if (left.parts.isEmpty() && !right.parts.isEmpty()) {
            return -1;
        }
        if (!left.parts.isEmpty() && right.parts.isEmpty()) {
            return 1;
        }
        int max = Math.max(left.parts.size(), right.parts.size());
        for (int i = 0; i < max; ++i) {
            int b;
            int a = i < left.parts.size() ? left.parts.get(i) : 0;
            int n = b = i < right.parts.size() ? right.parts.get(i) : 0;
            if (a == b) continue;
            return Integer.compare(a, b);
        }
        if (left.stageRank != right.stageRank) {
            return Integer.compare(left.stageRank, right.stageRank);
        }
        String leftNormalized = ModrinthUpdateManager.normalizeVersionForComparison(installed);
        String rightNormalized = ModrinthUpdateManager.normalizeVersionForComparison(remote);
        return leftNormalized.compareTo(rightNormalized);
    }

    private static boolean isReleaseType(String versionType) {
        String normalized = versionType == null ? "" : versionType.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || normalized.equals("release") || normalized.equals("stable");
    }

    private static String normalizeVersionForComparison(String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        int bracketIndex = normalized.indexOf(" [");
        if (bracketIndex >= 0) {
            normalized = normalized.substring(0, bracketIndex);
        }
        if (normalized.startsWith("v") && normalized.length() > 1 && Character.isDigit(normalized.charAt(1))) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String getInstalledVersionRaw() {
        Optional modContainer = FabricLoader.getInstance().getModContainer(PROJECT_SLUG);
        return modContainer.map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    private static String getCurrentGameVersion() {
        Optional minecraftContainer = FabricLoader.getInstance().getModContainer("minecraft");
        return minecraftContainer.map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("1.21");
    }

    private static List<String> getRequestedGameVersions() {
        return List.of(ModrinthUpdateManager.getCurrentGameVersion());
    }

    private static boolean isCompatibleWithCurrentGameVersion(RemoteVersion version, String currentGameVersion) {
        if (version == null) {
            return false;
        }
        if (currentGameVersion == null || currentGameVersion.isBlank()) {
            return true;
        }
        if (version.gameVersions.isEmpty()) {
            return true;
        }
        for (String gameVersion : version.gameVersions) {
            if (!currentGameVersion.equalsIgnoreCase(gameVersion)) continue;
            return true;
        }
        return false;
    }

    private static Path getReplacementTarget() {
        Path fromOrigin = ModrinthUpdateManager.getReplacementTargetFromModOrigin();
        if (fromOrigin != null) {
            return fromOrigin;
        }
        try {
            CodeSource codeSource = ModrinthUpdateManager.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return null;
            }
            return ModrinthUpdateManager.normalizeJarLikePath(Path.of(codeSource.getLocation().toURI()));
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static Path getReplacementTargetFromModOrigin() {
        try {
            Optional modContainer = FabricLoader.getInstance().getModContainer(PROJECT_SLUG);
            if (modContainer.isEmpty()) {
                return null;
            }
            Object origin = ((ModContainer)modContainer.get()).getClass().getMethod("getOrigin", new Class[0]).invoke(modContainer.get(), new Object[0]);
            if (origin == null) {
                return null;
            }
            Object pathsObject = origin.getClass().getMethod("getPaths", new Class[0]).invoke(origin, new Object[0]);
            if (!(pathsObject instanceof Iterable)) {
                return null;
            }
            Iterable iterable = (Iterable)pathsObject;
            for (Object entry : iterable) {
                Path path;
                Path normalized;
                if (!(entry instanceof Path) || (normalized = ModrinthUpdateManager.normalizeJarLikePath(path = (Path)entry)) == null) continue;
                return normalized;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private static Path normalizeJarLikePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            if (Files.isRegularFile(path, new LinkOption[0]) && path.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return path;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            int bang;
            String inner;
            Path extracted;
            String uri = path.toUri().toString();
            if (uri.startsWith("jar:") && Files.isRegularFile(extracted = Path.of(URI.create(inner = (bang = uri.indexOf(33)) >= 0 ? uri.substring(4, bang) : uri.substring(4))), new LinkOption[0]) && extracted.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return extracted;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private static Path writeInstallerScript(Path source, Path target, long pid, String versionNumber) throws IOException {
        Files.createDirectories(UPDATE_DIR, new FileAttribute[0]);
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (windows) {
            Path script = UPDATE_DIR.resolve("install_update.ps1");
            StringBuilder content = new StringBuilder();
            content.append("$PidToWait = ").append(pid).append("\r\n");
            content.append("$Source = '").append(ModrinthUpdateManager.escapePowerShell(source.toString())).append("'\r\n");
            content.append("$Target = '").append(ModrinthUpdateManager.escapePowerShell(target.toString())).append("'\r\n");
            content.append("$VersionNumber = '").append(ModrinthUpdateManager.escapePowerShell(versionNumber)).append("'\r\n");
            content.append("$LogPath = '").append(ModrinthUpdateManager.escapePowerShell(INSTALL_LOG.toString())).append("'\r\n");
            content.append("function Write-InstallLog([string]$Message) { Add-Content -LiteralPath $LogPath -Value ('[' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '] ' + $Message) }\r\n");
            content.append("Write-InstallLog 'Installer started'\r\n");
            content.append("for ($i = 0; $i -lt 240; $i++) {\r\n");
            content.append("  if (-not (Get-Process -Id $PidToWait -ErrorAction SilentlyContinue)) { break }\r\n");
            content.append("  Start-Sleep -Milliseconds 500\r\n");
            content.append("}\r\n");
            content.append("if (-not (Test-Path -LiteralPath $Source)) { Write-InstallLog 'Source file missing'; exit 1 }\r\n");
            content.append("$TempTarget = $Target + '.new'\r\n");
            content.append("for ($attempt = 1; $attempt -le 240; $attempt++) {\r\n");
            content.append("  try {\r\n");
            content.append("    Copy-Item -LiteralPath $Source -Destination $TempTarget -Force\r\n");
            content.append("    if (Test-Path -LiteralPath $Target) { Remove-Item -LiteralPath $Target -Force }\r\n");
            content.append("    Move-Item -LiteralPath $TempTarget -Destination $Target -Force\r\n");
            content.append("    if (Test-Path -LiteralPath $Source) { Remove-Item -LiteralPath $Source -Force }\r\n");
            content.append("    Write-InstallLog ('Installed version ' + $VersionNumber)\r\n");
            content.append("    exit 0\r\n");
            content.append("  } catch {\r\n");
            content.append("    Write-InstallLog ('Attempt ' + $attempt + ' failed: ' + $_.Exception.Message)\r\n");
            content.append("    try { if (Test-Path -LiteralPath $TempTarget) { Remove-Item -LiteralPath $TempTarget -Force } } catch {}\r\n");
            content.append("    Start-Sleep -Milliseconds 500\r\n");
            content.append("  }\r\n");
            content.append("}\r\n");
            content.append("Write-InstallLog 'Installer gave up after repeated retries'\r\n");
            content.append("exit 1\r\n");
            Files.writeString(script, (CharSequence)content.toString(), StandardCharsets.UTF_8, new OpenOption[0]);
            return script;
        }
        Path script = UPDATE_DIR.resolve("install_update.sh");
        StringBuilder content = new StringBuilder();
        content.append("#!/bin/sh\n");
        content.append("PID_TO_WAIT='").append(pid).append("'\n");
        content.append("SOURCE='").append(ModrinthUpdateManager.escapeSh(source.toString())).append("'\n");
        content.append("TARGET='").append(ModrinthUpdateManager.escapeSh(target.toString())).append("'\n");
        content.append("VERSION='").append(ModrinthUpdateManager.escapeSh(versionNumber)).append("'\n");
        content.append("LOG_PATH='").append(ModrinthUpdateManager.escapeSh(INSTALL_LOG.toString())).append("'\n");
        content.append("log(){ printf '[%s] %s\\n' \"$(date '+%Y-%m-%d %H:%M:%S')\" \"$1\" >> \"$LOG_PATH\"; }\n");
        content.append("log 'Installer started'\n");
        content.append("i=0\n");
        content.append("while kill -0 \"$PID_TO_WAIT\" 2>/dev/null; do\n");
        content.append("  i=$((i+1))\n");
        content.append("  [ \"$i\" -ge 240 ] && break\n");
        content.append("  sleep 0.5\n");
        content.append("done\n");
        content.append("[ -f \"$SOURCE\" ] || { log 'Source file missing'; exit 1; }\n");
        content.append("TEMP_TARGET=\"$TARGET.new\"\n");
        content.append("attempt=1\n");
        content.append("while [ \"$attempt\" -le 240 ]; do\n");
        content.append("  if cp -f \"$SOURCE\" \"$TEMP_TARGET\" 2>> \"$LOG_PATH\" && rm -f \"$TARGET\" 2>> \"$LOG_PATH\" && mv -f \"$TEMP_TARGET\" \"$TARGET\" 2>> \"$LOG_PATH\"; then\n");
        content.append("    rm -f \"$SOURCE\" 2>> \"$LOG_PATH\"\n");
        content.append("    log \"Installed version $VERSION\"\n");
        content.append("    exit 0\n");
        content.append("  fi\n");
        content.append("  rm -f \"$TEMP_TARGET\" 2>/dev/null\n");
        content.append("  log \"Attempt $attempt failed\"\n");
        content.append("  attempt=$((attempt+1))\n");
        content.append("  sleep 0.5\n");
        content.append("done\n");
        content.append("log 'Installer gave up after repeated retries'\n");
        content.append("exit 1\n");
        Files.writeString(script, (CharSequence)content.toString(), StandardCharsets.UTF_8, new OpenOption[0]);
        script.toFile().setExecutable(true);
        return script;
    }

    private static Path writeWindowsLauncher(Path script) throws IOException {
        Path launcher = UPDATE_DIR.resolve("install_update.vbs");
        String command = "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File \"" + script.toString() + "\"";
        StringBuilder content = new StringBuilder();
        content.append("Set shell = CreateObject(\"WScript.Shell\")\r\n");
        content.append("shell.Run \"").append(ModrinthUpdateManager.escapeVbs(command)).append("\", 0, False\r\n");
        Files.writeString(launcher, (CharSequence)content.toString(), StandardCharsets.UTF_8, new OpenOption[0]);
        return launcher;
    }

    private static void launchInstaller(Path script) throws IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (windows) {
            Path launcher = ModrinthUpdateManager.writeWindowsLauncher(script);
            new ProcessBuilder("wscript.exe", "//B", "//Nologo", launcher.toString()).start();
            return;
        }
        new ProcessBuilder("sh", script.toString()).start();
    }

    private static void openUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            class_156.method_668().method_670(url);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static String encodeJsonArray(List<String> values) {
        return URLEncoder.encode(GSON.toJson(values), StandardCharsets.UTF_8);
    }

    private static JsonArray toJsonArray(List<String> values) {
        JsonArray jsonArray = new JsonArray();
        for (String value : values) {
            jsonArray.add(value);
        }
        return jsonArray;
    }

    private static String sha1(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream inputStream = Files.newInputStream(path, new OpenOption[0]);){
            int read;
            byte[] buffer = new byte[8192];
            while ((read = inputStream.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder out = new StringBuilder();
        for (byte b : hash) {
            out.append(String.format(Locale.ROOT, "%02x", b));
        }
        return out.toString();
    }

    private static Instant parseInstant(String text) {
        try {
            return Instant.parse(text);
        }
        catch (Exception ignored) {
            return Instant.EPOCH;
        }
    }

    private static JsonArray getArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static JsonObject getObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static List<String> getStringList(JsonObject object, String key) {
        JsonArray array = ModrinthUpdateManager.getArray(object, key);
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<String>();
        for (JsonElement element : array) {
            String value;
            if (element == null || element.isJsonNull() || (value = element.getAsString()) == null || value.isBlank()) continue;
            out.add(value);
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static String getString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString() : fallback;
    }

    private static boolean getBoolean(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    private static long getLong(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsLong();
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private static String shortMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private static String escapeSh(String value) {
        return value.replace("'", "'\"'\"'");
    }

    private static String escapePowerShell(String value) {
        return value.replace("'", "''");
    }

    private static String escapeVbs(String value) {
        return value.replace("\"", "\"\"");
    }

    private static void appendInstallLog(String message) {
        try {
            Files.createDirectories(UPDATE_DIR, new FileAttribute[0]);
            Files.writeString(INSTALL_LOG, (CharSequence)("[" + String.valueOf(Instant.now()) + "] " + message + System.lineSeparator()), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void bumpRevision() {
        ++revision;
    }

    static {
        errorMessage = "";
        statusMessage = "Checking for updates...";
        versions = List.of();
    }

    public static final class RemoteVersion {
        public final String id;
        public final String name;
        public final String versionNumber;
        public final String versionType;
        public final Instant datePublished;
        public final String fileUrl;
        public final String fileName;
        public final String changelog;
        public final long fileSize;
        public final String sha1;
        public final String versionPageUrl;
        public final List<String> gameVersions;

        public RemoteVersion(String id, String name, String versionNumber, String versionType, Instant datePublished, String fileUrl, String fileName, String changelog, long fileSize, String sha1, String versionPageUrl, List<String> gameVersions) {
            this.id = id;
            this.name = name;
            this.versionNumber = versionNumber;
            this.versionType = versionType;
            this.datePublished = datePublished;
            this.fileUrl = fileUrl;
            this.fileName = fileName;
            this.changelog = changelog;
            this.fileSize = fileSize;
            this.sha1 = sha1;
            this.versionPageUrl = versionPageUrl;
            this.gameVersions = gameVersions == null ? List.of() : List.copyOf(gameVersions);
        }
    }

    private static final class ComparableVersion {
        private final List<Integer> parts;
        private final int stageRank;

        private ComparableVersion(List<Integer> parts, int stageRank) {
            this.parts = parts;
            this.stageRank = stageRank;
        }

        private static ComparableVersion parse(String raw) {
            int spaceIndex;
            int dashIndex;
            String normalized = ModrinthUpdateManager.normalizeVersionForComparison(raw);
            String base = normalized;
            int plusIndex = base.indexOf(43);
            if (plusIndex >= 0) {
                base = base.substring(0, plusIndex);
            }
            if ((dashIndex = base.indexOf(45)) >= 0) {
                base = base.substring(0, dashIndex);
            }
            if ((spaceIndex = base.indexOf(32)) >= 0) {
                base = base.substring(0, spaceIndex);
            }
            ArrayList<Integer> numbers = new ArrayList<Integer>();
            Matcher matcher = DIGITS.matcher(base);
            while (matcher.find()) {
                try {
                    numbers.add(Integer.parseInt(matcher.group()));
                }
                catch (NumberFormatException numberFormatException) {}
            }
            String stageSource = normalized;
            if (stageSource.contains("alpha") || stageSource.contains("snapshot") || stageSource.contains("pre") || stageSource.contains("preview")) {
                return new ComparableVersion(numbers, 0);
            }
            if (stageSource.contains("beta")) {
                return new ComparableVersion(numbers, 1);
            }
            if (stageSource.contains("rc")) {
                return new ComparableVersion(numbers, 2);
            }
            return new ComparableVersion(numbers, 3);
        }
    }
}
