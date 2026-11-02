package dev.xardas.nekoroqol.mixin.client;

import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.xardas.nekoroqol.client.ToastFilter;

/** Every toast, vanilla or modded, goes through addToast; hidden ones are simply never added. */
@Mixin(ToastComponent.class)
public abstract class ToastComponentMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void nekoroQol$hideToast(Toast toast, CallbackInfo ci) {
        if (ToastFilter.hide(toast)) ci.cancel();
    }
}
