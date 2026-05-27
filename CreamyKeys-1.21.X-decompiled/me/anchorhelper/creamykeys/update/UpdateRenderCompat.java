package me.anchorhelper.creamykeys.update;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;

public final class UpdateRenderCompat {
    private static final int WARNING_ICON_TEXTURE_SIZE = 16;
    private static final class_2960 WARNING_ICON_DYNAMIC_ID = class_2960.method_60655((String)"creamykeys", (String)"dynamic/warningicon");
    private static final List<class_2960> WARNING_ICON_IDS = List.of(class_2960.method_60655((String)"warningicon", (String)"warningicon.png"), class_2960.method_60655((String)"warningicon", (String)"textures/warningicon.png"), class_2960.method_60655((String)"warningicon", (String)"textures/gui/warningicon.png"), class_2960.method_60655((String)"creamykeys", (String)"warningicon.png"), class_2960.method_60655((String)"creamykeys", (String)"textures/warningicon.png"), class_2960.method_60655((String)"creamykeys", (String)"textures/gui/warningicon.png"));
    private static class_2960 warningIconTextureId;
    private static String warningIconSourceKey;

    private UpdateRenderCompat() {
    }

    public static void drawTextWithShadow(class_332 context, class_327 textRenderer, class_2561 text, int x, int y, int color) {
        if (context == null || textRenderer == null) {
            return;
        }
        if (text == null) {
            text = class_2561.method_43473();
        }
        if (UpdateRenderCompat.invokeText(context, textRenderer, text, x, y, color = UpdateRenderCompat.withFullAlpha(color))) {
            return;
        }
        UpdateRenderCompat.invokeString(context, textRenderer, text.getString(), x, y, color);
    }

    public static void drawTextWithShadow(class_332 context, class_327 textRenderer, String text, int x, int y, int color) {
        if (context == null || textRenderer == null) {
            return;
        }
        if (text == null) {
            text = "";
        }
        if (UpdateRenderCompat.invokeString(context, textRenderer, text, x, y, color = UpdateRenderCompat.withFullAlpha(color))) {
            return;
        }
        UpdateRenderCompat.invokeText(context, textRenderer, (class_2561)class_2561.method_43470((String)text), x, y, color);
    }

    public static void drawCenteredTextWithShadow(class_332 context, class_327 textRenderer, class_2561 text, int centerX, int y, int color) {
        if (text == null) {
            text = class_2561.method_43473();
        }
        int x = centerX - textRenderer.method_1727(text.getString()) / 2;
        UpdateRenderCompat.drawTextWithShadow(context, textRenderer, text, x, y, color);
    }

    public static void drawCenteredTextWithShadow(class_332 context, class_327 textRenderer, String text, int centerX, int y, int color) {
        if (text == null) {
            text = "";
        }
        int x = centerX - textRenderer.method_1727(text) / 2;
        UpdateRenderCompat.drawTextWithShadow(context, textRenderer, text, x, y, color);
    }

    public static void drawTooltip(class_332 context, class_327 textRenderer, class_2561 text, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (text == null) {
            text = class_2561.method_43473();
        }
        String line = text.getString();
        int padding = 4;
        int width = textRenderer.method_1727(line) + padding * 2;
        Objects.requireNonNull(textRenderer);
        int height = 9 + padding * 2;
        int x = mouseX + 10;
        int y = mouseY - height - 6;
        if (x + width > screenWidth - 6) {
            x = screenWidth - width - 6;
        }
        if (x < 6) {
            x = 6;
        }
        if (y < 6) {
            y = mouseY + 10;
        }
        if (y + height > screenHeight - 6) {
            y = screenHeight - height - 6;
        }
        context.method_25294(x - 1, y - 1, x + width + 1, y + height + 1, -1073741824);
        context.method_25294(x, y, x + width, y + height, -267386864);
        context.method_25294(x, y, x + width, y + 1, 0x60FFFFFF);
        context.method_25294(x, y + height - 1, x + width, y + height, 0x60000000);
        context.method_25294(x, y, x + 1, y + height, 0x60FFFFFF);
        context.method_25294(x + width - 1, y, x + width, y + height, 0x60000000);
        UpdateRenderCompat.drawTextWithShadow(context, textRenderer, line, x + padding, y + padding, -1);
    }

    public static void drawWarningIcon(class_332 context, int x, int y, int size) {
        class_2960 texture = UpdateRenderCompat.getWarningIconTexture();
        if (texture != null && UpdateRenderCompat.tryDrawTexture(context, texture, x, y, size)) {
            return;
        }
        UpdateRenderCompat.drawFallbackWarning(context, x, y, size);
    }

    private static class_2960 getWarningIconTexture() {
        class_310 client = class_310.method_1551();
        if (client == null) {
            return null;
        }
        for (class_2960 sourceId : WARNING_ICON_IDS) {
            class_2960 class_29602;
            block13: {
                class_1043 texture;
                InputStream stream;
                String sourceKey;
                block12: {
                    Optional resource = client.method_1478().method_14486(sourceId);
                    if (resource.isEmpty()) continue;
                    sourceKey = sourceId.toString();
                    if (WARNING_ICON_DYNAMIC_ID.equals((Object)warningIconTextureId) && sourceKey.equals(warningIconSourceKey)) {
                        return warningIconTextureId;
                    }
                    Object resourceObject = resource.get();
                    InputStream inputStream = UpdateRenderCompat.openResourceStream(resourceObject);
                    if (inputStream == null) continue;
                    stream = inputStream;
                    class_1011 image = class_1011.method_4309((InputStream)stream);
                    texture = UpdateRenderCompat.createBackedTexture(image);
                    if (texture != null) break block12;
                    if (stream == null) continue;
                    stream.close();
                    continue;
                }
                try {
                    client.method_1531().method_4616(WARNING_ICON_DYNAMIC_ID, (class_1044)texture);
                    warningIconTextureId = WARNING_ICON_DYNAMIC_ID;
                    warningIconSourceKey = sourceKey;
                    class_29602 = warningIconTextureId;
                    if (stream == null) break block13;
                }
                catch (Throwable throwable) {
                    try {
                        if (stream != null) {
                            try {
                                stream.close();
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                    catch (Exception exception) {}
                }
                stream.close();
            }
            return class_29602;
        }
        return warningIconTextureId;
    }

    private static class_1043 createBackedTexture(class_1011 image) {
        try {
            for (Constructor<?> constructor : class_1043.class.getConstructors()) {
                Object value;
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 1 && params[0] == class_1011.class && (value = constructor.newInstance(image)) instanceof class_1043) {
                    class_1043 texture = (class_1043)value;
                    return texture;
                }
                if (params.length == 2 && Supplier.class.isAssignableFrom(params[0]) && params[1] == class_1011.class && (value = constructor.newInstance(() -> "creamykeys_warning_icon", image)) instanceof class_1043) {
                    class_1043 texture = (class_1043)value;
                    return texture;
                }
                if (params.length != 2 || params[0] != String.class || params[1] != class_1011.class || !((value = constructor.newInstance("creamykeys_warning_icon", image)) instanceof class_1043)) continue;
                class_1043 texture = (class_1043)value;
                return texture;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private static InputStream openResourceStream(Object resourceObject) {
        if (resourceObject == null) {
            return null;
        }
        try {
            Method method = resourceObject.getClass().getMethod("getInputStream", new Class[0]);
            method.setAccessible(true);
            Object result = method.invoke(resourceObject, new Object[0]);
            if (result instanceof InputStream) {
                InputStream stream = (InputStream)result;
                return stream;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private static boolean tryDrawTexture(class_332 context, class_2960 texture, int x, int y, int size) {
        Object guiTextured = UpdateRenderCompat.findGuiTexturedPipeline();
        if (guiTextured != null && UpdateRenderCompat.tryInvokePipelineDrawTexture(context, guiTextured, texture, x, y, size)) {
            return true;
        }
        if (UpdateRenderCompat.tryInvokeLegacyDrawTexture(context, texture, x, y, size)) {
            return true;
        }
        return guiTextured != null && UpdateRenderCompat.tryInvokeDrawGuiTexture(context, guiTextured, texture, x, y, size);
    }

    private static boolean tryInvokeDrawGuiTexture(class_332 context, Object guiTextured, class_2960 texture, int x, int y, int size) {
        for (Method method : context.getClass().getMethods()) {
            if (!method.getName().equals("drawGuiTexture")) continue;
            Class<?>[] params = method.getParameterTypes();
            try {
                if (params.length == 6 && params[1] == class_2960.class && params[2] == Integer.TYPE && params[3] == Integer.TYPE && params[4] == Integer.TYPE && params[5] == Integer.TYPE && params[0].isInstance(guiTextured)) {
                    method.setAccessible(true);
                    method.invoke((Object)context, guiTextured, texture, x, y, size, size);
                    return true;
                }
                if (params.length == 10 && params[1] == class_2960.class && params[2] == Integer.TYPE && params[3] == Integer.TYPE && params[4] == Integer.TYPE && params[5] == Integer.TYPE && params[6] == Integer.TYPE && params[7] == Integer.TYPE && params[8] == Integer.TYPE && params[9] == Integer.TYPE && params[0].isInstance(guiTextured)) {
                    method.setAccessible(true);
                    method.invoke((Object)context, guiTextured, texture, 16, 16, 0, 0, x, y, size, size);
                    return true;
                }
                if (params.length != 11 || params[1] != class_2960.class || params[2] != Integer.TYPE || params[3] != Integer.TYPE || params[4] != Integer.TYPE || params[5] != Integer.TYPE || params[6] != Integer.TYPE || params[7] != Integer.TYPE || params[8] != Integer.TYPE || params[9] != Integer.TYPE || params[10] != Integer.TYPE || !params[0].isInstance(guiTextured)) continue;
                method.setAccessible(true);
                method.invoke((Object)context, guiTextured, texture, 16, 16, 0, 0, x, y, size, size, -1);
                return true;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return false;
    }

    private static boolean tryInvokePipelineDrawTexture(class_332 context, Object guiTextured, class_2960 texture, int x, int y, int size) {
        for (Method method : context.getClass().getMethods()) {
            if (!method.getName().equals("drawTexture")) continue;
            Class<?>[] params = method.getParameterTypes();
            try {
                if (params.length == 12 && params[1] == class_2960.class && params[2] == Integer.TYPE && params[3] == Integer.TYPE && params[4] == Float.TYPE && params[5] == Float.TYPE && params[6] == Integer.TYPE && params[7] == Integer.TYPE && params[8] == Integer.TYPE && params[9] == Integer.TYPE && params[10] == Integer.TYPE && params[11] == Integer.TYPE && params[0].isInstance(guiTextured)) {
                    method.setAccessible(true);
                    method.invoke((Object)context, guiTextured, texture, x, y, Float.valueOf(0.0f), Float.valueOf(0.0f), size, size, 16, 16, 16, 16);
                    return true;
                }
                if (params.length != 13 || params[1] != class_2960.class || params[2] != Integer.TYPE || params[3] != Integer.TYPE || params[4] != Float.TYPE || params[5] != Float.TYPE || params[6] != Integer.TYPE || params[7] != Integer.TYPE || params[8] != Integer.TYPE || params[9] != Integer.TYPE || params[10] != Integer.TYPE || params[11] != Integer.TYPE || params[12] != Integer.TYPE || !params[0].isInstance(guiTextured)) continue;
                method.setAccessible(true);
                method.invoke((Object)context, guiTextured, texture, x, y, Float.valueOf(0.0f), Float.valueOf(0.0f), size, size, 16, 16, 16, 16, -1);
                return true;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return false;
    }

    private static boolean tryInvokeLegacyDrawTexture(class_332 context, class_2960 texture, int x, int y, int size) {
        for (Method method : context.getClass().getMethods()) {
            if (!method.getName().equals("drawTexture")) continue;
            Class<?>[] params = method.getParameterTypes();
            try {
                if (params.length == 11 && params[0] == class_2960.class && params[1] == Integer.TYPE && params[2] == Integer.TYPE && params[3] == Integer.TYPE && params[4] == Integer.TYPE && params[5] == Float.TYPE && params[6] == Float.TYPE && params[7] == Integer.TYPE && params[8] == Integer.TYPE && params[9] == Integer.TYPE && params[10] == Integer.TYPE) {
                    method.setAccessible(true);
                    method.invoke((Object)context, texture, x, y, size, size, Float.valueOf(0.0f), Float.valueOf(0.0f), 16, 16, 16, 16);
                    return true;
                }
                if (params.length != 12 || params[0] != class_2960.class || params[1] != Integer.TYPE || params[2] != Integer.TYPE || params[3] != Integer.TYPE || params[4] != Integer.TYPE || params[5] != Integer.TYPE || params[6] != Float.TYPE || params[7] != Float.TYPE || params[8] != Integer.TYPE || params[9] != Integer.TYPE || params[10] != Integer.TYPE || params[11] != Integer.TYPE) continue;
                method.setAccessible(true);
                method.invoke((Object)context, texture, x, y, 0, size, size, Float.valueOf(0.0f), Float.valueOf(0.0f), 16, 16, 16, 16);
                return true;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return false;
    }

    private static Object findGuiTexturedPipeline() {
        Object pipeline = UpdateRenderCompat.findStaticField("net.minecraft.client.gl.RenderPipelines", "GUI_TEXTURED");
        if (pipeline != null) {
            return pipeline;
        }
        pipeline = UpdateRenderCompat.findStaticField("net.minecraft.class_10799", "field_56883");
        if (pipeline != null) {
            return pipeline;
        }
        pipeline = UpdateRenderCompat.findStaticField("com.mojang.blaze3d.pipeline.RenderPipelines", "GUI_TEXTURED");
        if (pipeline != null) {
            return pipeline;
        }
        return UpdateRenderCompat.findStaticField("com.mojang.blaze3d.systems.RenderPipelines", "GUI_TEXTURED");
    }

    private static Object findStaticField(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            Field field = type.getField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static void drawFallbackWarning(class_332 context, int x, int y, int size) {
        context.method_25294(x, y, x + size, y + size, -2056192);
        context.method_25294(x + 1, y + 1, x + size - 1, y + size - 1, -669621);
        int exclamationWidth = Math.max(1, size / 5);
        int exclamationTop = y + Math.max(2, size / 6);
        int exclamationBottom = y + size - Math.max(5, size / 4);
        int exclamationX = x + (size - exclamationWidth) / 2;
        context.method_25294(exclamationX, exclamationTop, exclamationX + exclamationWidth, exclamationBottom, -14671840);
        int dotSize = Math.max(1, size / 6);
        int dotX = x + (size - dotSize) / 2;
        int dotY = y + size - Math.max(2, size / 5) - dotSize;
        context.method_25294(dotX, dotY, dotX + dotSize, dotY + dotSize, -14671840);
    }

    private static boolean invokeText(class_332 context, class_327 textRenderer, class_2561 text, int x, int y, int color) {
        return UpdateRenderCompat.invoke(context, "drawTextWithShadow", new Class[]{class_327.class, class_2561.class, Integer.TYPE, Integer.TYPE, Integer.TYPE}, new Object[]{textRenderer, text, x, y, color}) || UpdateRenderCompat.invoke(context, "method_27535", new Class[]{class_327.class, class_2561.class, Integer.TYPE, Integer.TYPE, Integer.TYPE}, new Object[]{textRenderer, text, x, y, color});
    }

    private static boolean invokeString(class_332 context, class_327 textRenderer, String text, int x, int y, int color) {
        return UpdateRenderCompat.invoke(context, "drawTextWithShadow", new Class[]{class_327.class, String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE}, new Object[]{textRenderer, text, x, y, color}) || UpdateRenderCompat.invoke(context, "method_25303", new Class[]{class_327.class, String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE}, new Object[]{textRenderer, text, x, y, color});
    }

    private static boolean invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(target, args);
            return true;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private static int withFullAlpha(int color) {
        return (color & 0xFF000000) == 0 ? 0xFF000000 | color : color;
    }

    static {
        warningIconSourceKey = "";
    }
}
