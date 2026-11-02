package dev.xardas.nekoroqol.client;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import dev.xardas.nekoroqol.QolConfig;

/**
 * A short F3 screen with what players actually look at: FPS, coordinates, direction, biome, day and time,
 * light. A key switches to the full screen (vanilla and other mods' lines), coloured so the useful lines
 * stand out and the technical ones fade into dark grey.
 */
final class DebugScreen {
    private static final String LABEL = "§7";
    private static final String VALUE = "§f";
    private static final String POSITION = "§b";
    private static final String WORLD = "§a";
    private static final String TIME = "§6";
    private static final String LIGHT = "§e";
    private static final String FADED = "§8";
    private static final String[] MOON = {"Full Moon", "Waning Gibbous", "Third Quarter", "Waning Crescent",
            "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous"};

    /** Full screen shown; goes back to the short one whenever F3 is closed. */
    static boolean full;

    private DebugScreen() { }

    /** The key: opens F3 in the full view, or switches between short and full while it is open. */
    static void toggleFull(Minecraft mc) {
        if (!mc.getDebugOverlay().showDebugScreen()) {
            mc.getDebugOverlay().toggleOverlay();
            full = true;
        } else {
            full = !full;
        }
    }

    static void onTick(Minecraft mc) {
        if (full && !mc.getDebugOverlay().showDebugScreen()) full = false;
    }

    static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!QolConfig.get().shortDebugScreen) return;
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        ClientLevel level = mc.level;
        if (camera == null || level == null) return;
        if (full) {
            colour(event.getLeft(), true, mc, level, camera);
            colour(event.getRight(), false, mc, level, camera);
        } else {
            event.getLeft().clear();
            event.getRight().clear();
            shortScreen(event.getLeft(), mc, level, camera);
        }
    }

    private static void shortScreen(List<String> out, Minecraft mc, ClientLevel level, Entity camera) {
        BlockPos pos = camera.blockPosition();
        out.add(LABEL + "FPS: " + fps(mc.getFps()) + mc.getFps());
        if (!mc.showOnlyReducedInfo()) {
            out.add(LABEL + "XYZ: " + POSITION + pos.getX() + " / " + pos.getY() + " / " + pos.getZ());
        }
        out.add(LABEL + "Facing: " + POSITION + facing(camera.getDirection()));
        if (!mc.showOnlyReducedInfo()) out.add(LABEL + "Biome: " + WORLD + biome(level.getBiome(pos)));
        out.add(LABEL + "Time: " + TIME + dayAndTime(level));
        out.add(LABEL + "Light: " + LIGHT + light(level, pos));
    }

    private static String fps(int fps) {
        return fps >= 60 ? "§a" : fps >= 30 ? "§e" : "§c";
    }

    private static String facing(Direction direction) {
        String axis = switch (direction) {
            case NORTH -> "-Z";
            case SOUTH -> "+Z";
            case WEST -> "-X";
            case EAST -> "+X";
            default -> "";
        };
        String name = direction.getName();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " (" + axis + ")";
    }

    private static String biome(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> {
            ResourceLocation id = key.location();
            String translation = "biome." + id.getNamespace() + "." + id.getPath();
            return Language.getInstance().has(translation) ? Language.getInstance().getOrDefault(translation) : id.toString();
        }).orElse("?");
    }

    private static String dayAndTime(ClientLevel level) {
        long time = level.getDayTime();
        long day = time / 24000 + 1;
        // tick 0 of a day is 6:00 in the morning
        long ofDay = (time + 6000) % 24000;
        return String.format(Locale.ROOT, "Day %d, %02d:%02d", day, ofDay / 1000, ofDay % 1000 * 60 / 1000);
    }

    private static String light(ClientLevel level, BlockPos pos) {
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos);
        return Math.max(block, sky - level.getSkyDarken()) + VALUE + " (block " + block + ", sky " + sky + ")";
    }

    private static String weather(ClientLevel level) {
        return level.isThundering() ? "Thunder" : level.isRaining() ? "Rain" : "Clear";
    }

    /** Colours the full screen; lines that already have colours (from other mods) keep them. */
    private static void colour(List<String> lines, boolean left, Minecraft mc, ClientLevel level, Entity camera) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty() || line.startsWith("§")) continue;
            lines.set(i, colourFor(line, left, i) + line);
            if (left && line.startsWith("Biome: ")) {
                lines.add(i + 1, LABEL + "Time: " + TIME + dayAndTime(level) + LABEL + "  Moon: " + TIME
                        + MOON[level.getMoonPhase()] + LABEL + "  Weather: " + TIME + weather(level));
                i++;
            }
        }
    }

    private static String colourFor(String line, boolean left, int index) {
        if (!left) {
            return line.startsWith("Mem:") || line.startsWith("Allocated:") ? LIGHT : FADED;
        }
        if (index == 1 && line.contains(" fps")) {
            try {
                return fps(Integer.parseInt(line.substring(0, line.indexOf(' '))));
            } catch (NumberFormatException e) {
                return VALUE;
            }
        }
        if (line.startsWith("XYZ:") || line.startsWith("Block:") || line.startsWith("Chunk:") || line.startsWith("Facing:")) {
            return POSITION;
        }
        if (line.startsWith("Biome:") || line.startsWith("Local Difficulty:")) return WORLD;
        if (line.startsWith("Client Light:") || line.startsWith("Server Light:")) return LIGHT;
        if (line.startsWith("Integrated server") || line.startsWith("\"")) return VALUE;
        if (line.startsWith("[")) return LABEL;
        return FADED;
    }
}
