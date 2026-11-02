package dev.xardas.nekoroqol;

import net.minecraft.server.level.ServerPlayer;

/** Every recipe is known: on joining and after /reload (the datapack sync happens in both cases). */
public final class RecipeUnlock {
    private RecipeUnlock() { }

    public static void onDatapackSync(ServerPlayer player) {
        if (!QolConfig.get().unlockAllRecipes) return;
        player.awardRecipes(player.server.getRecipeManager().getRecipes());
    }
}
