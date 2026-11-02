package dev.xardas.nekoroqol.mixin.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets QolClient recognise the recipe book button by its sprites. */
@Mixin(ImageButton.class)
public interface ImageButtonAccessor {
    @Accessor("sprites")
    WidgetSprites nekoroQol$getSprites();
}
