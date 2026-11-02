package dev.xardas.nekoroqol.fabric;

import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

import dev.xardas.nekoroqol.Platform;

public final class FabricPlatform implements Platform {
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean allowXpPickup(Player player, ExperienceOrb orb) {
        return true;
    }
}
