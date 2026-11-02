package dev.xardas.nekoroqol;

import java.nio.file.Path;
import java.util.ServiceLoader;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

/** The few things that differ between NeoForge and Fabric. Each loader project provides one implementation. */
public interface Platform {
    Platform INSTANCE = load(Platform.class);

    static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, type.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + type.getSimpleName() + " for this mod loader"));
    }

    Path configDir();

    /** Other mods may stop an XP pickup (NeoForge has an event for it); true where nothing can. */
    boolean allowXpPickup(Player player, ExperienceOrb orb);
}
