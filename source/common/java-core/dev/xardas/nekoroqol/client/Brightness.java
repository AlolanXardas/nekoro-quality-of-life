package dev.xardas.nekoroqol.client;

import java.util.Optional;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import dev.xardas.nekoroqol.QolConfig;

/**
 * The Brightness slider going past 100%. The light map already brightens further when gamma is above 1, it is
 * only the slider that stops there. Around 1150% even the darkest spot is fully lit.
 */
public final class Brightness {
    private Brightness() { }

    /** The vanilla gamma option, with a range up to brightness_max. Same caption and the same value in options.txt. */
    public static OptionInstance<Double> option() {
        double max = QolConfig.get().brightnessMax / 100.0;
        int maxPercent = QolConfig.get().brightnessMax;
        return new OptionInstance<>("options.gamma", OptionInstance.noTooltip(), (caption, value) -> {
            int percent = (int) Math.round(value * 100);
            if (percent == 0) return Options.genericValueLabel(caption, Component.translatable("options.gamma.min"));
            if (percent == 50) return Options.genericValueLabel(caption, Component.translatable("options.gamma.default"));
            if (percent == 100) return Options.genericValueLabel(caption, Component.translatable("options.gamma.max"));
            if (percent >= maxPercent && maxPercent > 100) {
                return Options.genericValueLabel(caption, Component.translatable("options.nekoro_qol.gamma.full"));
            }
            return Options.genericValueLabel(caption, percent);
        }, new Range(max), 0.5, value -> { });
    }

    private record Range(double max) implements OptionInstance.SliderableValueSet<Double> {
        @Override
        public double toSliderValue(Double value) {
            return value / max;
        }

        /** Whole percent steps, and a snap to 100% so the vanilla "Bright" value is easy to hit. */
        @Override
        public Double fromSliderValue(double slider) {
            double value = Math.round(slider * max * 100) / 100.0;
            return Math.abs(value - 1) < max * 0.01 ? 1.0 : value;
        }

        @Override
        public Optional<Double> validateValue(Double value) {
            return Optional.of(Mth.clamp(value, 0, max));
        }

        @Override
        public Codec<Double> codec() {
            return Codec.withAlternative(Codec.DOUBLE, Codec.BOOL, on -> on ? 1.0 : 0.0)
                    .xmap(value -> Mth.clamp(value, 0, max), value -> value);
        }
    }
}
