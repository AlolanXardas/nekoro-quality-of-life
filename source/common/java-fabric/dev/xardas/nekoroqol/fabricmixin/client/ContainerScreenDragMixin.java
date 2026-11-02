package dev.xardas.nekoroqol.fabricmixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroqol.client.InventoryMouse;

/** Shift + drag in inventories (Fabric API has no drag event). */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenDragMixin {
    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void nekoroQol$drag(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        InventoryMouse.onDrag((Screen) (Object) this, mouseX, mouseY, button);
    }
}
