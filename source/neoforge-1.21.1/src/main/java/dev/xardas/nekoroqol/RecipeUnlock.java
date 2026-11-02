package dev.xardas.nekoroqol;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/** Every recipe is known: on joining and after /reload (the datapack sync happens in both cases). */
final class RecipeUnlock {
    private RecipeUnlock() { }

    static void onDatapackSync(OnDatapackSyncEvent event) {
        if (!QolConfig.get().unlockAllRecipes) return;
        var recipes = event.getPlayerList().getServer().getRecipeManager().getRecipes();
        event.getRelevantPlayers().forEach((ServerPlayer player) -> player.awardRecipes(recipes));
    }
}
