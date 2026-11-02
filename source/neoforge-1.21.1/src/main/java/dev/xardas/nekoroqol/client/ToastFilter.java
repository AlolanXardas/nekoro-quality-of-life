package dev.xardas.nekoroqol.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import dev.xardas.nekoroqol.QolConfig;

/** Decides which toasts never show, and posts the text of hidden mod toasts to chat if wanted. */
public final class ToastFilter {
    /** The same text goes to chat at most once in this time, because some mods add their toast again and again. */
    private static final long CHAT_COOLDOWN_MS = 5 * 60 * 1000;
    private static final Map<String, Long> lastSent = new HashMap<>();

    private ToastFilter() { }

    public static boolean hide(Toast toast) {
        QolConfig cfg = QolConfig.get();
        if (cfg.hideTutorials && toast instanceof TutorialToast) return true;
        if (cfg.hideAdvancements && toast instanceof AdvancementToast) return true;
        if (cfg.hideRecipeToasts && toast instanceof RecipeToast) return true;
        if (!cfg.hiddenToastClasses.isEmpty() && matches(cfg.hiddenToastClasses, toast.getClass())) {
            if (cfg.hiddenToastsToChat) sendToChat(toast);
            return true;
        }
        return false;
    }

    private static boolean matches(List<String> names, Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            if (names.contains(c.getName())) return true;
        }
        return false;
    }

    /** Toasts have no common way to read their text, so every Component field of the toast is used, in order. */
    private static void sendToChat(Toast toast) {
        List<Component> parts = new ArrayList<>();
        for (Class<?> c = toast.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!Component.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(toast);
                    if (value instanceof Component text && !text.getString().isBlank()) parts.add(text);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // a field we may not read; the toast stays hidden, only its text is missing
                }
            }
        }
        if (parts.isEmpty() || Minecraft.getInstance().player == null) return;
        MutableComponent message = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) message.append(Component.literal(" - "));
            message.append(parts.get(i));
        }
        String key = message.getString();
        long now = System.currentTimeMillis();
        Long last = lastSent.get(key);
        if (last != null && now - last < CHAT_COOLDOWN_MS) return;
        lastSent.put(key, now);
        Minecraft.getInstance().player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), false);
    }
}
