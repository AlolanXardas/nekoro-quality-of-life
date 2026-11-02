package dev.xardas.nekoroqol.mixin.client;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.xardas.nekoroqol.QolConfig;

/**
 * With the recipe book hidden, its contents are never built. Sorting every recipe into book tabs is slow
 * in modpacks with thousands of recipes; the book then just stays empty (its lists start out empty).
 */
@Mixin(ClientRecipeBook.class)
public abstract class ClientRecipeBookMixin {
    @Inject(method = "setupCollections", at = @At("HEAD"), cancellable = true)
    private void nekoroQol$skipRecipeBook(Iterable<RecipeHolder<?>> recipes, RegistryAccess registries, CallbackInfo ci) {
        if (QolConfig.get().hideRecipeBook) ci.cancel();
    }
}
